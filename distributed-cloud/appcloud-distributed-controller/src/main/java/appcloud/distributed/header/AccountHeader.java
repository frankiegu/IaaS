package appcloud.distributed.header;

import appcloud.netty.remoting.common.ConsumerHeader;
import lombok.Data;

/**
 * Created by Idan on 2018/1/15.
 */
@Data
public class AccountHeader extends ConsumerHeader{

    private String requestId;
    private String regionId;
    private String zoneId;
    private String newUserEmail;
    private String groupSecretKey;
    private String appkeyId;
    private String appkeySecret;
    private String userEmail;
    private String accountName;

    public AccountHeader() {
    }

    public AccountHeader(String requestId, String regionId, String zoneId, String newUserEmail, String groupSecretKey, String appkeyId, String appkeySecret, String accountName) {
        this.requestId = requestId;
        this.regionId = regionId;
        this.zoneId = zoneId;
        this.newUserEmail = newUserEmail;
        this.groupSecretKey = groupSecretKey;
        this.appkeyId = appkeyId;
        this.appkeySecret = appkeySecret;
        this.accountName = accountName;
    }

    public AccountHeader(String requestId, String regionId, String zoneId,String newUserEmail, String groupSecretKey, String appkeyId, String appkeySecret, String userEmail,String accountName) {
        this.requestId = requestId;
        this.regionId = regionId;
        this.zoneId = zoneId;
        this.newUserEmail = newUserEmail;
        this.groupSecretKey = groupSecretKey;
        this.appkeyId = appkeyId;
        this.appkeySecret = appkeySecret;
        this.userEmail = userEmail;
        this.accountName = accountName;
    }

    @Override
    public void checkFileds() {}
}
