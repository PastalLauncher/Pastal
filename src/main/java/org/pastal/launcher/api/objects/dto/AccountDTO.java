package org.pastal.launcher.api.objects.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.pastal.launcher.api.interfaces.Account;
import org.pastal.launcher.api.interfaces.PremiumAccount;

@Data
@AllArgsConstructor
public class AccountDTO {
    private String username;
    private String type;
    private String uuid;
    private boolean premium;

    public static AccountDTO convertToDTO(Account account) {
        return new AccountDTO(
                account.getUsername(),
                account.getTypeName(),
                account.getUUID().toString(),
                account instanceof PremiumAccount
        );
    }
}