package org.ifsoft.ohun.openfire;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletRequest;
import javax.security.auth.Subject;

import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.security.*;

import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.auth.AuthToken;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.admin.AdminManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.XMPPServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A login service that uses Jetty to authenticate Openfire users
 *
 */

public class OhunLoginService extends AbstractLifeCycle implements LoginService
{
    private static final Logger Log = LoggerFactory.getLogger(OhunLoginService.class);
    public static final ConcurrentHashMap<String, String> passwords = new ConcurrentHashMap<>();

    private IdentityService _identityService = new DefaultIdentityService();
    private String _name;
    private UserManager userManager = XMPPServer.getInstance().getUserManager();

    protected OhunLoginService()
    {

    }

    public OhunLoginService(String name)
    {
        setName(name);
    }

    public String getName()
    {
        return _name;
    }

    public IdentityService getIdentityService()
    {
        return _identityService;
    }

    public void setIdentityService(IdentityService identityService)
    {
        if (isRunning())
            throw new IllegalStateException("Running");
        _identityService = identityService;
    }

    public void setName(String name)
    {
        if (isRunning())
            throw new IllegalStateException("Running");
        _name = name;
    }


    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
    }

    public void logout(UserIdentity identity)
    {
        Log.debug("logout {}",identity);
    }

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName()+"["+_name+"]";
    }

    public UserIdentity login(String userName, Object credential)
    {
        return login(userName, credential, null);
    }

    public UserIdentity login(String userName, Object credential, ServletRequest request)
    {
        Log.debug("UserIdentity login " + userName + " ");

        UserIdentity identity = null;

        try {
            userManager.getUser(userName);
        }

        catch (UserNotFoundException e) {
            Log.warn( "user not found " + userName, e );
            return null;
        }

        try {
            AuthFactory.authenticate( userName, (String) credential);
            passwords.put(userName, (String) credential);

        } catch ( UnauthorizedException e ) {
            Log.error( "access denied, bad password " + userName );
            return null;

        } catch ( Exception e ) {
            Log.error( "access denied " + userName );
            return null;
        }

        Principal userPrincipal = new KnownUser(userName, credential);
        Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        subject.getPrivateCredentials().add(credential);
        subject.getPrincipals().add(new RolePrincipal("ohun"));
        subject.setReadOnly();

        identity = _identityService.newUserIdentity(subject, userPrincipal, new String[] {"ohun"});
        return identity;
    }

    public boolean validate(UserIdentity user)
    {
        return true;
    }

    public static class KnownUser implements UserPrincipal, Serializable
    {
        private static final long serialVersionUID = -6226920753748399662L;
        private final String _name;
        private final Object _credential;

        public KnownUser(String name, Object credential)
        {
            _name=name;
            _credential=credential;
        }

        public boolean authenticate(Object credentials)
        {
            return true;
        }

        public String getName()
        {
            return _name;
        }

        public boolean isAuthenticated()
        {
            return true;
        }

        @Override public String toString()
        {
            return _name;
        }
    }

    public interface UserPrincipal extends Principal,Serializable
    {
        boolean authenticate(Object credentials);
        public boolean isAuthenticated();
    }

    public static class RolePrincipal implements Principal,Serializable
    {
        private static final long serialVersionUID = 2998397924051854403L;
        private final String _roleName;

        public RolePrincipal(String name)
        {
            _roleName=name;
        }
        public String getName()
        {
            return _roleName;
        }
    }
}

