package hello.login.domain.login;

import hello.login.domain.member.Member;
import hello.login.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * 로그인을 위한 판단로직(비지니스 로직)이 필요함
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final MemberRepository memberRepository;

    /**
     *
     * @param loginId
     * @param password
     * @return null이면 로그인 실패
     */
    public Member login(String loginId,String password){
//        Optional<Member> findMemberOptional = memberRepository.findByLoginId(loginId);
//        Member member = findMemberOptional.get();//Optional에서 get이라고 하면 꺼낼수 있다.
//        if(member.getPassword().equals(password)){
//            return member;
//        }else{
//            return null;
//        }
        //-위코드와 동일한 부분 java8 Optional 에서 지원하는 코드임 꼭 알아볼것
         return memberRepository.findByLoginId(loginId)
                 .filter(m->m.getPassword().equals(password))
                .orElse(null);
    }

}
