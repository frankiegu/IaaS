package appcloud.distributed.nativeConfig;

import appcloud.distributed.util.Command;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 在DNS服务器设置ip与域名
 * Created by zouji on 2017/12/6.
 */
public class DnsContentConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(DnsContentConfig.class);

    private SshExector sshExector = new SshExector();

    public void startSshExector() {
        sshExector.initSession();
    }

    public void stopSshExector() {
        sshExector.close();
    }
    /**
     * setIpBindDomainName
     * @param domainName
     * @param ipAddress
     * @return
     */
    public Boolean setIpDomain(String domainName, String ipAddress) {
        DnsDirector dnsDirector = new DnsDirector(domainName, ipAddress);
        if (isZoneExsit(domainName) && !appendZone(dnsDirector)) {
            LOGGER.info("domain already exist in dns config");
            return false;
        }
        return writeZoneFile(dnsDirector);
    }

    private Boolean writeZoneFile(DnsDirector dnsDirector) {
        try {
            String dnsContent = dnsDirector.genConfigStr();
            String command = Command.createZoneFile(dnsContent,dnsDirector.getFilename());
            sshExector.executeSSHCommand(command);
            String restart = "service named restart";
            sshExector.executeSSHCommand(restart);
        } catch (IOException e) {
            LOGGER.error("writeZoneFile:Write zone failed!",e);
            return false;
        } catch (JSchException e) {
            LOGGER.error("appendZone:SSH create failed!", e);
            return false;
        }
        return true;
    }

    private Boolean appendZone(DnsDirector dnsDirector) {
        try {
            String dnsContent = dnsDirector.genConfigStr();
            String command = Command.createDnsZone(dnsContent);
            sshExector.executeSSHCommand(command);
        } catch (IOException e) {
            LOGGER.error("appendZone:Write zone failed!" + dnsDirector.genConfigStr());
            return false;
        } catch (JSchException e) {
            LOGGER.error("appendZone:SSH create failed!",e);
            return false;
        }
        return true;
    }

    private Boolean isZoneExsit(String domainName) {

        String command = "cat /etc/named.conf | grep named."+ domainName;
        try {
            String response = sshExector.executeSSHCommand(command);
            return response.contains(domainName);//true则存在
        } catch (JSchException e) {
            LOGGER.error("isZoneExsit:SSH create failed!",e);
            return true;
        } catch (IOException e) {
            LOGGER.error("isZoneExsit:Write zone failed!",e);
            return true;
        }
    }

    public static void main(String[] args) {
        DnsContentConfig dnsContentConfig = new DnsContentConfig();
        System.out.println(dnsContentConfig.isZoneExsit("free4lab.com"));

    }
}
