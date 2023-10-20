package eskit.sdk.support.messenger.client.utils;

import android.text.TextUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

/**
 *
 */
public class NetUtils {

    private static final String[] NET_FILTER = new String[]{"rmnet", "vmnet", "vnic", "vboxnet", "virtual", "ppp"};

    public static String getIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            brk:
            while (interfaces.hasMoreElements()) {
                NetworkInterface _interface = interfaces.nextElement();
                if (!_interface.isUp()) continue;
                if (_interface.isLoopback()) continue;
                for (String filter : NET_FILTER) {
                    if (_interface.getName().toLowerCase(Locale.ROOT).startsWith(filter)) {
                        continue brk;
                    }
                }
                Enumeration<InetAddress> inetAddresses = _interface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isLoopbackAddress()) continue;
                    String ip = inetAddress.getHostAddress();
                    if (!TextUtils.isEmpty(ip) && ip.contains(".")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
