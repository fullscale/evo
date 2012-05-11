package co.diji.cloud9.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class Cloud9User implements UserDetails {

    private static final long serialVersionUID = 1L;
    private String userName = "admin";
    private String password = "4efe081594ce25ee4efd9f7067f7f678a347bccf2de201f3adf2a3eb544850b465b4e51cdc3fcdde";
    private GrantedAuthority[] authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
        roles.add(new SimpleGrantedAuthority("supervisor"));
        roles.add(new SimpleGrantedAuthority("user"));
        roles.add(new SimpleGrantedAuthority("teller"));
        return roles;
    }

    public void setCustomerAuthorities(List<String> roles) {
        List<GrantedAuthority> listOfAuthorities = new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            listOfAuthorities.add(new SimpleGrantedAuthority(role));
        }
        authorities = (GrantedAuthority[]) listOfAuthorities.toArray();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
