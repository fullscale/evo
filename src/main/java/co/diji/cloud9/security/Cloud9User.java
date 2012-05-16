package co.diji.cloud9.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class Cloud9User implements UserDetails {

    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private boolean accountNonExpired = true;
    private boolean nonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    private ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    public void setAuthorities(ArrayList<String> roles) {
        for (String role : roles) {
            addAuthority(role);
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

}
