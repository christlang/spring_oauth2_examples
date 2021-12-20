package de.synyx.cl.oauth.examples.service.a;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Controller
public class InfoController {

    @GetMapping("/")
    public String info(Model model, @AuthenticationPrincipal OAuth2User principal) {

        Collection<SimpleGrantedAuthority> authoritiesSimple = (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        List<String> list = new ArrayList<>();
        final Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        authoritiesSimple.forEach(auth -> list.add("authority: " + auth.toString()));

        list.add("---------");

        authorities.forEach(auth -> list.add("from principal: " + auth.toString()));
        model.addAttribute("infos", list);
        return "info.html";
    }
}
