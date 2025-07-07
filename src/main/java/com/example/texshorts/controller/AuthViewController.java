//package com.example.texshorts.controller;
//
//import com.example.texshorts.DTO.SignupRequest;
//import com.example.texshorts.entity.Gender;
//import com.example.texshorts.entity.User;
//import com.example.texshorts.repository.UserRepository;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PostMapping;
//
//import java.util.List;
//
//@Controller
//public class AuthViewController {
//    private final PasswordEncoder passwordEncoder;
//    private final UserRepository userRepository;
//
//    public AuthViewController(PasswordEncoder passwordEncoder, UserRepository userRepository) {
//        this.passwordEncoder = passwordEncoder;
//        this.userRepository = userRepository;
//    }
//
//    @GetMapping("/signup")
//    public String signupForm(Model model) {
//        model.addAttribute("signupRequest", new SignupRequest());
//        model.addAttribute("genders", Gender.values());
//        return "signup"; // resources/templates/signup.html
//    }
//
//    @PostMapping("/signup")
//    public String signupSubmit(@ModelAttribute SignupRequest signupRequest, Model model) {
//        if (userRepository.findByUsername(signupRequest.getUsername()).isPresent()) {
//            model.addAttribute("error", "이미 존재하는 아이디입니다.");
//            model.addAttribute("genders", Gender.values());
//            return "signup";
//        }
//
//        User user = new User();
//        user.setUsername(signupRequest.getUsername());
//        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
//        user.setEmail(signupRequest.getEmail());
//        user.setGender(signupRequest.getGender());
//        user.setBirthDate(signupRequest.getBirthDate());
//        user.setRoles(List.of("ROLE_USER"));
//
//        userRepository.save(user);
//
//        return "redirect:/login";
//    }
//
//}
