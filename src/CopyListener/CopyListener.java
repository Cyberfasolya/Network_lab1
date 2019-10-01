package CopyListener;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static CopyListener.Properties.ONE_SECOND;
import static CopyListener.Properties.PORT_NUMBER;
import static CopyListener.Properties.UUID_BYTE_LENGTH;


public class CopyListener {
    private MulticastSocket socket;
    private String myUuid;
    private DatagramPacket sendPacket;
    private DatagramPacket receivePacket;
    private HashMap<String, Info> copiesMap;

    public CopyListener(String address) throws IllegalStateException, IllegalArgumentException {
        try {
            socket = new MulticastSocket(PORT_NUMBER);
        } catch (IOException e) {
            throw new IllegalStateException("Socket creating with port number " + PORT_NUMBER + " failed.");
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Address resolving by name/address " + address + "failed.");
        }

        try {
            socket.joinGroup(inetAddress);
        } catch (IOException e) {
            throw new IllegalStateException("Join group failed.");
        }

        myUuid = UUID.randomUUID().toString();
        System.out.println("my myUuid: " + myUuid + "\n");

        byte[] receiveBuf = new byte[UUID_BYTE_LENGTH];
        sendPacket = new DatagramPacket(myUuid.getBytes(), UUID_BYTE_LENGTH, inetAddress, PORT_NUMBER);
        receivePacket = new DatagramPacket(receiveBuf, UUID_BYTE_LENGTH);

        copiesMap = new HashMap<>();
    }

    public void run() throws IllegalStateException {

        long lastSendTime;
        boolean timeOutExpired;

        while (true) {
            send();

            lastSendTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - lastSendTime < ONE_SECOND) {

                timeOutExpired = !receive();
                if (timeOutExpired) {
                    continue;
                }

                String receivedUuid = new String(receivePacket.getData());
                if (!isForeignUuid(receivedUuid)) {
                    continue;
                }

                updateInfo(receivedUuid);
            }

            removeDead();
            printInfo();
        }
    }

    private void send() throws IllegalStateException {
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            throw new IllegalStateException("Error while trying send.");
        }
    }

    private boolean receive() {
        try {
            socket.setSoTimeout(ONE_SECOND);
            socket.receive(receivePacket);
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean isForeignUuid(String uuid) {
        return !myUuid.equals(uuid);
    }

    private void updateInfo(String receivedUuid) {

        if (!copiesMap.containsKey(receivedUuid)) {
            Info currentInfo = new Info();
            currentInfo.IP = receivePacket.getAddress().getHostAddress();
            currentInfo.lastTime = System.currentTimeMillis();
            copiesMap.put(receivedUuid, currentInfo);
        } else {
            copiesMap.get(receivedUuid).lastTime = System.currentTimeMillis();
        }
    }

    private void removeDead() {
        List<String> infoToDelete = copiesMap
                .entrySet()
                .stream()
                .filter(entry -> System.currentTimeMillis() - entry.getValue().lastTime > 3 * ONE_SECOND)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        infoToDelete.forEach(copiesMap::remove);
    }

    private void printInfo() {
        if (!copiesMap.isEmpty()) {
            System.out.println("\n copies:");
            copiesMap.forEach((key, value) -> System.out.println(value.IP + "    " + key));
        } else {
            System.out.println("\n no copies");
        }
    }

    private static class Info {
        String IP;
        Long lastTime;
    }


}
