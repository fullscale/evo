package co.diji.cloud9.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class Cloud9UserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String arg0) throws UsernameNotFoundException {
        return new Cloud9User();
    }

}
