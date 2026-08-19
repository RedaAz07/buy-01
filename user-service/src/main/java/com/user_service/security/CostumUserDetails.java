package com.user_service.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.user_service.model.User;
import com.user_service.repository.UserRespository;

public class CostumUserDetails implements UserDetailsService {

    private final UserRespository userRepository;

    public CostumUserDetails(UserRespository userRespository) {
        this.userRepository = userRespository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        return new org.springframework.security.core.userdetails.User(
                user.getName(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())));

    }

}
