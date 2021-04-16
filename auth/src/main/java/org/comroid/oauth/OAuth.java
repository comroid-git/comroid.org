package org.comroid.oauth;

import org.comroid.oauth.user.OAuthBlob;
import org.comroid.oauth.user.OAuthUser;
import org.zalando.stups.tokens.*;

public final class OAuth {
    private OAuth() {
        throw new UnsupportedOperationException();
    }

    public interface User extends UserCredentials {
    }

    public interface UserProvider extends User, UserCredentialsProvider {
        OAuthUser getOAuthUser();

        @Override
        default UserCredentials get() throws CredentialsUnavailableException {
            return this;
        }
    }

    public interface Service extends ClientCredentials {
    }

    public interface ServiceProvider extends Service, ClientCredentialsProvider {
        @Override
        default ClientCredentials get() throws CredentialsUnavailableException {
            return this;
        }
    }
}
