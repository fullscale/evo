package co.fs.evo.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class EvoUser implements UserDetails {

    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String uid;
    private boolean accountNonExpired = true;
    private boolean nonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    private ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }
    
    public String[] getRoles() {
        String [] roles = new String[this.authorities.size()];
        int idx = 0;
        for (SimpleGrantedAuthority role : this.authorities) {
        	roles[idx] = role.getAuthority();
        	idx += 1;
        }
        return roles;
    }

    public void setAuthorities(ArrayList<String> roles) {
    	authorities.clear();
        for (String role : roles) {
            addAuthority(role);
        }
    }
    
    public void setAuthorities(String[] roles) {
    	authorities.clear();
    	for (int i = 0; i < roles.length; i++) {
    		addAuthority(roles[i]);
    	}
    }

    public void addAuthority(String authority) {
        authorities.add(new SimpleGrantedAuthority(authority));
    }

    public void removeAuthority(String authority) {
        authorities.remove(authority);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return this.uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.nonLocked;
    }

    public void setAccountNonLocked(boolean nonLocked) {
        this.nonLocked = nonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Map<String, Object> asMap() {
    	
    	Map<String, Object> user = new HashMap<String, Object>();
    	user.put("username", getUsername());
    	user.put("uid", getUid());
    	user.put("authorities", getRoles());
    	user.put("accountNonExpired", isAccountNonExpired());
    	user.put("accountNonLocked", isAccountNonLocked());
    	user.put("credentialsNonExpired", isCredentialsNonExpired());
    	user.put("enabled", isEnabled());
    	
    	return user;
    }

    public String toString() {
    	return new JSONObject(asMap()).toJSONString();
    }
}
