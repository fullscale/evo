package co.fs.evo.security;

import static co.fs.evo.Constants.SYSTEM_INDEX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
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

import co.fs.evo.services.SearchService;

public class EvoUserDetailsService implements UserDetailsService, UserDetailsManager {

    private static final XLogger logger = XLoggerFactory.getXLogger(EvoUserDetailsService.class);
    private AuthenticationManager authenticationManager;
    private UserCache userCache = new NullUserCache();

    @Autowired
    private SearchService searchService;

    /**
     * Returns the user account details as a Java Map.
     */
    private Map<String, Object> getUser(String userName) {
        GetResponse user = searchService.getDoc(SYSTEM_INDEX, "users", userName, null);
        return user.sourceAsMap();
    }

    /**
     * Returns the account details for the given user.
     */
    @SuppressWarnings("unchecked")
    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        logger.entry(userName);
        Map<String, Object> source = getUser(userName);
        EvoUser principle = null;

        if (source != null) {
            principle = new EvoUser();
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

        logger.exit();
        return principle;
    }

    /**
     * Adds the new user to the system account structure.
     */
    private void addUser(UserDetails user) {
        logger.entry();
        Map<String, Object> source = new HashMap<String, Object>();
        source.put("uid", ((EvoUser)user).getUid());
        source.put("username", user.getUsername());
        source.put("password", user.getPassword());
        source.put("authorities", ((EvoUser)user).getRoles());
        source.put("accountNonExpired", user.isAccountNonExpired());
        source.put("accountNonLocked", user.isAccountNonLocked());
        source.put("credentialsNonExpired", user.isCredentialsNonExpired());
        source.put("enabled", user.isEnabled());
        searchService.indexDoc(SYSTEM_INDEX, "users", user.getUsername(), source);
        searchService.refreshIndex(SYSTEM_INDEX);
        logger.exit();
    }

    /**
     * Creates a new account for the given user.
     */
    public void createUser(UserDetails user) {
        if (userExists(user.getUsername()) == false) {
            addUser(user);
        }
    }

    /**
     * Updates the account for the given user.
     */
    public void updateUser(final UserDetails user) {
        addUser(user);
    }

    /**
     * Deletes the given user account
     */
    public void deleteUser(String username) {
        logger.entry(username);
        if (!username.equals("admin")) {
        	searchService.deleteDoc(SYSTEM_INDEX, "users", username);
        	searchService.refreshIndex(SYSTEM_INDEX);
        }
        logger.exit();
    }

    /**
     * Checks whether the given user exists.
     */
    public boolean userExists(String userName) {
        GetResponse user = searchService.getDoc(SYSTEM_INDEX, "users", userName, null);
        return user.exists();
    }
    
    /**
     * Returns a list of the first 250 users (uses match_all query)
     * 
     * See {@link co.fs.evo.services.SearchService#matchAll()}
     */
    public List<Map<String, Object>> listUsers() {
    	SearchResponse response = searchService.matchAll(SYSTEM_INDEX, 
    			"users", new String[]{"uid", "username", "authorities"});
    	
    	List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
    	
    	for (SearchHit hit : response.hits().hits()) {
    		Map<String, Object> m = new HashMap<String, Object>();
    		m.put("uid", hit.field("uid").getValue());
    		m.put("username", hit.field("username").getValue());
    		m.put("authorities", hit.field("authorities").getValue());
            users.add(m);
    	}
    	
    	return users;
    }

    /**
     * This method changes the password for the currently logged in user.
     * It mainly meant to allow users to change their own password. It 
     * will re-authenticate the users automatically using the new password.
     */
    public void changePassword(String oldPassword, String newPassword) 
    		throws AuthenticationException {
    	
        logger.entry();
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();

        if (currentUser == null) {
            // This would indicate bad coding somewhere
            throw new AccessDeniedException("Can't change password as no Authentication object found in context "
                    + "for current user.");
        }

        String username = currentUser.getName();

        // If an authentication manager has been set, re-authenticate the user with the supplied password.
        if (authenticationManager != null) {
            logger.debug("Reauthenticating user '" + username + "' for password change request.");

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, oldPassword));
        } else {
            logger.debug("No authentication manager set. Password won't be re-checked.");
        }

        EvoUser newUser = (EvoUser) loadUserByUsername(username);
        newUser.setPassword(newPassword);
        updateUser(newUser);

        SecurityContextHolder.getContext().setAuthentication(createNewAuthentication(currentUser, newUser, newPassword));
        userCache.removeUserFromCache(username);
        logger.exit();
    }

    /**
     * Used to set the current authentication manager. 
     */
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Creates a new Authentication object.
     */
    protected Authentication createNewAuthentication(Authentication currentUser, UserDetails newUser, String newPassword) {
        logger.entry();
        UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(newUser,
                newUser.getPassword(), newUser.getAuthorities());
        newAuthentication.setDetails(currentUser.getDetails());

        logger.exit();
        return newAuthentication;
    }

    /**
     * Sets the cache object that will be used to manage users.
     */
    public void setUserCache(UserCache userCache) {
        Assert.notNull(userCache, "userCache cannot be null");
        this.userCache = userCache;
    }
}
