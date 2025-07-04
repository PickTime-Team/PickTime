package com.b101.pickTime.api.user.service.impl;


import com.b101.pickTime.api.user.request.PasswordCheckReq;
import com.b101.pickTime.api.user.request.PasswordUpdateReq;
import com.b101.pickTime.api.user.request.UserModiftReqDto;
import com.b101.pickTime.api.user.request.UserRegisterReq;
import com.b101.pickTime.api.user.response.UserInfoDto;
import com.b101.pickTime.api.user.service.UserService;
import com.b101.pickTime.common.auth.CustomUserDetails;
import com.b101.pickTime.common.exception.exception.DuplicateEmailException;
import com.b101.pickTime.common.exception.exception.PasswordNotChangedException;
import com.b101.pickTime.common.exception.exception.PasswordNotMatchedException;
import com.b101.pickTime.db.entity.User;
import com.b101.pickTime.db.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    // 실구현체(BCryptPasswordEncoder)가 아닌 인터페이스(PasswordEncoder)를 주입해야 함
    private final PasswordEncoder passwordEncoder;

    public void createUser(UserRegisterReq userRegisterReq) {
        String username = userRegisterReq.getUsername();
        // 이미 존재하는 이메일인지 확인
        if(isExistUsername(username)) {
            throw new DuplicateEmailException();
        }
        
        User user = User.builder()
                .username(userRegisterReq.getUsername())
                .password(passwordEncoder.encode(userRegisterReq.getPassword()))
                .name(userRegisterReq.getName())
//                .level(1)
//                .role(Role.ROLE_USER)
//                .isActive(true)
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoDto getUser(int userId) {
        return userRepository.getUserByUserId(userId);
    }

    @Override
    public UserInfoDto modifyUser(int userId, UserModiftReqDto userModiftReqDto) {
        User user = getUserEntity(userId);

        user.setName(userModiftReqDto.getName());
        User modifiedUser = userRepository.save(user);

        return new UserInfoDto(modifiedUser.getUsername(), modifiedUser.getName(), modifiedUser.getLevel());
    }

    @Override
    public void unactivateUser(int userId) {
        User user = getUserEntity(userId);
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void checkPassword(PasswordCheckReq passwordCheckReq, Integer userId) {
        // DB에서 조회하여 비교 <= customerUserDetails에서 바로 비교 X
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(passwordCheckReq.getPassword(), user.getPassword())) {
            throw new PasswordNotMatchedException("password is not matched");
        }
    }
    public void modifyPassword(PasswordUpdateReq passwordUpdateReq, Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        // 이전 패스워드와 동일한가
        if (passwordEncoder.matches(passwordUpdateReq.getPassword(), user.getPassword())) {
            throw new PasswordNotChangedException("password is same compared with previous password");
        }
        user.updatePassword(passwordEncoder.encode(passwordUpdateReq.getPassword()));
        userRepository.save(user);
    }
    public boolean isExistUsername(String username) {
        return userRepository.existsByUsername(username);
    }


    private User getUserEntity(int userId){
        return  userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보가 없습니다."))
                .checkActive();
    }
}
