package CopyListener;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CopyListener {
    private int PORT_NUMBER = 8888;
    private int UUID_BYTE_LENGTH = 36;
    private int oneSecond = 1000;

    public void run(String address) {
        MulticastSocket socket;
        try {
            socket = new MulticastSocket(PORT_NUMBER);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        try {
            socket.joinGroup(inetAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String uuid = UUID.randomUUID().toString();
        System.out.println("my uuid: " + uuid + "\n");
        byte[] receiveBuf = new byte[UUID_BYTE_LENGTH];
        DatagramPacket sendPacket = new DatagramPacket(uuid.getBytes(), UUID_BYTE_LENGTH, inetAddress, PORT_NUMBER);
        DatagramPacket receivePacket = new DatagramPacket(receiveBuf, UUID_BYTE_LENGTH);

        long lastSendTime;

        HashMap<String, Info> copiesMap = new HashMap<>();

        while (true) {
            try {
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            lastSendTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - lastSendTime < oneSecond) {
                try {
                    socket.setSoTimeout(oneSecond);
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                String currentUuid = new String(receivePacket.getData());
                if(currentUuid.equals(uuid)){
                    continue;
                }

                if (!copiesMap.containsKey(currentUuid)) {
                    Info currentInfo = new Info();
                    currentInfo.IP = receivePacket.getAddress().getHostAddress();
                    currentInfo.lastTime = System.currentTimeMillis();
                    copiesMap.put(currentUuid, currentInfo);
                } else {
                    copiesMap.get(currentUuid).lastTime = System.currentTimeMillis();
                }

            }

            List<String> infoToDelete = copiesMap
                    .entrySet()
                    .stream()
                    .filter(entry -> System.currentTimeMillis() - entry.getValue().lastTime > 3 * oneSecond)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            infoToDelete.forEach(copiesMap::remove);

            if(!copiesMap.isEmpty()){
                System.out.println("\n copies:");
            }
            copiesMap.forEach((key, value) -> System.out.println(value.IP + "    " + key));
        }
    }

    private static class Info {
        public String IP;
        public Long lastTime;
    }
}
