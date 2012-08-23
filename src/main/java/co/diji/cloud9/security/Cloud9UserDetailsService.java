package co.diji.cloud9.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserCache;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.cache.NullUserCache;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.util.Assert;

import co.diji.cloud9.services.SearchService;

public class Cloud9UserDetailsService implements UserDetailsService, UserDetailsManager {

    private static final XLogger logger = XLoggerFactory.getXLogger(Cloud9UserDetailsService.class);
    private AuthenticationManager authenticationManager;
    private UserCache userCache = new NullUserCache();

    @Autowired
    private SearchService searchService;

    private Map<String, Object> getUser(String userName) {
        GetResponse user = searchService.getDoc("sys", "users", userName, null);
        return user.sourceAsMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        Map<String, Object> source = getUser(userName);
        Cloud9User principle = null;

        if (source != null) {
            principle = new Cloud9User();
            principle.setUsername((String) source.get("username"));
            principle.setPassword((String) source.get("password"));
            principle.setUid((String) source.get("uid"));
            principle.setAccountNonExpired((Boolean) source.get("accountNonExpired"));
            principle.setAccountNonLocked((Boolean) source.get("accountNonLocked"));
            principle.setCredentialsNonExpired((Boolean) source.get("credentialsNonExpired"));
            principle.setEnabled((Boolean) source.get("enabled"));
            principle.setAuthorities((ArrayList<String>) source.get("authorities"));
        } else {
            throw new UsernameNotFoundException("Unknown user: " + userName);
        }

        return principle;
    }

    private void addUser(UserDetails user) {
        Map<String, Object> source = new HashMap<String, Object>();
        source.put("username", user.getUsername());
        source.put("password", user.getPassword());
        source.put("accountNonExpired", user.isAccountNonExpired());
        source.put("accountNonLocked", user.isAccountNonLocked());
        source.put("credentialsNonExpired", user.isCredentialsNonExpired());
        source.put("enabled", user.isEnabled());
        searchService.indexDoc("sys", "users", user.getUsername(), source);
    }

    public void createUser(UserDetails user) {
        if (userExists(user.getUsername()) == false) {
            addUser(user);
        }
    }

    public void updateUser(final UserDetails user) {
        addUser(user);
    }

    public void deleteUser(String username) {
        searchService.deleteDoc("sys", "users", username);
    }

    public boolean userExists(String userName) {
        Map<String, Object> source = getUser(userName);
        return source != null ? true : false;
    }

    public void changePassword(String oldPassword, String newPassword) throws AuthenticationException {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();

        if (currentUser == null) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException("Can't change password as no Authentication object found in context "
                    + "for current user.");
        }

        String username = currentUser.getName();

        // If an authentication manager has been set, reauthenticate the user with the supplied password.
        if (authenticationManager != null) {
            logger.debug("Reauthenticating user '" + username + "' for password change request.");

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, oldPassword));
        } else {
            logger.debug("No authentication manager set. Password won't be re-checked.");
        }

        Cloud9User newUser = (Cloud9User) loadUserByUsername(username);
        newUser.setPassword(newPassword);
        updateUser(newUser);

        SecurityContextHolder.getContext().setAuthentication(createNewAuthentication(currentUser, newUser, newPassword));
        userCache.removeUserFromCache(username);
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    protected Authentication createNewAuthentication(Authentication currentUser, UserDetails newUser, String newPassword) {

        UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(newUser,
                newUser.getPassword(), newUser.getAuthorities());
        newAuthentication.setDetails(currentUser.getDetails());

        return newAuthentication;
    }

    public void setUserCache(UserCache userCache) {
        Assert.notNull(userCache, "userCache cannot be null");
        this.userCache = userCache;
    }
}
