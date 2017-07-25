package gov.samhsa.c2s.masteruiapi.service;

import gov.samhsa.c2s.masteruiapi.config.C2sClientProperties;
import gov.samhsa.c2s.masteruiapi.infrastructure.SupportedRoles;
import gov.samhsa.c2s.masteruiapi.service.dto.LoginResponseDto;
import gov.samhsa.c2s.masteruiapi.service.dto.CredentialsDto;
import gov.samhsa.c2s.masteruiapi.service.dto.UaaTokenDto;
import gov.samhsa.c2s.masteruiapi.service.dto.UaaUserInfoDto;
import gov.samhsa.c2s.masteruiapi.service.dto.LimitedProfileResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UaaService uaaService;

    @Autowired
    private UmsService umsService;

    @Autowired
    private C2sClientProperties c2sClientProperties;

    @Override
    public LoginResponseDto login(CredentialsDto credentialsDto) {
        Optional<UaaTokenDto>  accessToken =  uaaService.getAccessTokenUsingPasswordGrant(credentialsDto);
        if(accessToken.isPresent()){
            Optional<UaaUserInfoDto> userInfo = uaaService.getUserInfo(accessToken);
            if(userInfo.isPresent() && (credentialsDto.getRole().equals(SupportedRoles.PATIENT.getName())
                    ||credentialsDto.getRole().equals(SupportedRoles.PROVIDER.getName()) )){

                System.out.println("I am in.");

                LimitedProfileResponse limitedProfileResponse = umsService.getProfile(userInfo.get().getUser_id(), userInfo.get().getUser_name());
                // Return token to user
                return LoginResponseDto.builder()
                        .accessToken(accessToken.get())
                        .profileToken(userInfo.get())
                        .limitedProfileResponse(limitedProfileResponse)
                        .c2sClientHomeUrl(getUiHomeUrlByRole(credentialsDto.getRole()))
                        .masterUiLoginUrl(c2sClientProperties.getMasterUi().getLoginUrl())
                        .build();
            }else if(userInfo.isPresent() && credentialsDto.getRole().equals(SupportedRoles.STAFF_USER.getName())){
                LimitedProfileResponse limitedProfileResponse = umsService.getStaffProfile();

                return LoginResponseDto.builder()
                        .accessToken(accessToken.get())
                        .profileToken(userInfo.get())
                        .limitedProfileResponse(limitedProfileResponse)
                        .c2sClientHomeUrl(getUiHomeUrlByRole(credentialsDto.getRole()))
                        .masterUiLoginUrl(c2sClientProperties.getMasterUi().getLoginUrl())
                        .build();
            }else {
                // TODO Throw exception: cannot get user info from UAA
                return null;
            }
        }else {
            // TODO Throw exception : cannot get token from UAA
            return  null;
        }
    }

    private String getUiHomeUrlByRole(String role){
        String homeUrl = null;
        if(role.equals(SupportedRoles.PATIENT.getName())){
            homeUrl =  c2sClientProperties.getC2sUi().getHomeUrl();
        }else  if(role.equals(SupportedRoles.PROVIDER.getName())){
            homeUrl =  c2sClientProperties.getProviderUi().getHomeUrl();
        }else  if(role.equals(SupportedRoles.STAFF_USER.getName())){
            homeUrl =  c2sClientProperties.getStaffUi().getHomeUrl();
        }
        return homeUrl;
    }
}
