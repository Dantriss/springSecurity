package com.example.springsecurity.oauth;

import com.example.springsecurity.auth.PrincipalDetails;
import com.example.springsecurity.model.User;
import com.example.springsecurity.oauth.provider.FacebookUserInfo;
import com.example.springsecurity.oauth.provider.GoogleUserInfo;
import com.example.springsecurity.oauth.provider.NaverUserInfo;
import com.example.springsecurity.oauth.provider.OAuth2UserInfo;
import com.example.springsecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    UserRepository userRepository;

    // 구글로 부터 받은 UserRequest 데이터에 대한 후처리 함수
    // 함수 종료시 @AuthenticationPrincipal 어노테이션이 만들어진다.
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    // 구글로그인 버튼 클릭 -> 구글로그인창-> 로그인완료 -> code를 리턴(OAuth-Client라이브러리)->AccessToken요청
    // userRequest 정보 -> loadUser함수호출 -> 구글에서 회원프로필 받음
    OAuth2User oAuth2User = super.loadUser(userRequest);
    System.out.println("getAttributes : "+oAuth2User.getAttributes());


        System.out.println("getAccessToken : "+userRequest.getAccessToken());
        System.out.println("getClientRegistration : "+userRequest.getClientRegistration().getRegistrationId());

        OAuth2UserInfo oAuth2UserInfo = null;
        if(userRequest.getClientRegistration().getRegistrationId().equals("google")){
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        } else if (userRequest.getClientRegistration().getRegistrationId().equals("facebook")) {
            oAuth2UserInfo = new FacebookUserInfo(oAuth2User.getAttributes());

        } else if (userRequest.getClientRegistration().getRegistrationId().equals("naver")) {
            oAuth2UserInfo = new NaverUserInfo((Map) oAuth2User.getAttributes().get("response"));

        }

        String provider = oAuth2UserInfo.getProvider();   //google,facebook
        String providerId = oAuth2UserInfo.getProviderId();
        String username = provider+"_"+providerId;                                    //google_0000000
        String password = bCryptPasswordEncoder.encode("1111");
        String email = oAuth2UserInfo.getEmail();
        String role = "ROLE_USER";

        User userEntity = userRepository.findByUsername(username);

        if (userEntity==null){
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .build();

            userRepository.save(userEntity);
        }

        return new PrincipalDetails(userEntity,oAuth2User.getAttributes());
    }
}
