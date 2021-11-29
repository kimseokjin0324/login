package hello.login.domain.member;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

//- 객체생성
@Data
public class Member {

    private Long id;            //-데이터베이스에 저장되는 아이디

    @NotEmpty
    private String loginId;     //-로그인 id
    @NotEmpty
    private String name;        //-사용자이름
    @NotEmpty
    private String password;
}
