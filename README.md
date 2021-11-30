# login
## 패키지 구조 설계
### package 구조
* hello.login
* domain
  - item
  - member
  - login
* web
  - web
  - member
  - login
#### 도메인이 가장 중요하다.
도메인=화면,UI,기술 인프라 등등의 영역은 제외한 시스템이 구현해야 하는 핵심 비지니스 업무 영역을 말함
향후 web을 다른 기술로 바꾸어도 도메인은 그대로 유지할 수 있어야한다.
이렇게 하려면 web은 domain을 알고있지만 domain은 web을 모르도록 설계해야한다. 이것을 web은 domain을 의존하지만, domain은 web을 의존하지 않는다고 표현한다.
예를 들어 web패키지를 모두 삭제해도 domain에는 전혀 영향이 없도록 의존관계를 설계하는 것이 중요하다. 반대로 이야기하면 domain은 web을 참조하면 안된다.
## 홈화면
홈 화면을 개발하자
### HomeController-home()수정

    @GetMapping("/")
    public String home() {
        return "home";
    }
    
## 회원가입 화면
## 로그인 화면
로그인을 위한 판단로직(비지니스 로직)이 필요함( domain/LoginService)
private final MemberRepository memberRepository;

    /**
     *
     * @param loginId
     * @param password
     * @return null이면 로그인 실패
     */
    public Member login(String loginId,String password){
        //-위코드와 동일한 부분 java8 Optional 에서 지원하는 코드임 꼭 알아볼것
         return memberRepository.findByLoginId(loginId)
                 .filter(m->m.getPassword().equals(password))
                .orElse(null);
    }
    
로그인의 핵심 비지니스 로직은 회원을 조회한 다음에 파라미터로 넘어온 password와 비교해서 같으면 회원을 반환하고 만약 password가 다르면 null을 반환

### LoginForm
@Data
public class LoginForm {

    @NotEmpty
    private String loginId;
    @NotEmpty
    private String password;
}

### LoginController
private final LoginService loginService;


    @GetMapping("/login")
    public String loginForm(@ModelAttribute("loginForm")LoginForm form){
        return "login/loginForm";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult){
        if(bindingResult.hasErrors()){
            return "login/loginForm";
        }

        Member loginMember = loginService.login(form.getLoginId(), form.getPassword());

        //-글로벌 오류
        if(loginMember==null){
            bindingResult.reject("loginFail"," 아이디 또는 비밀번호가 맞지 않습니다.");
            return "login/loginForm";
        }
        //로그인 성공 처리 TODO

        return "redirect:/";
    }

로그인 컨트롤러는 로그인 서비스를 호출해서 로그인에 성공하면 홈화면으로 이동하고, 로그인에 실패하면 bindingResult.reject를 사용해서 글로벌 오류를 생성한다. 그리고 정보를 다시 입력하도록 로그인 폼을 뷰 템플릿으로 한다. 

### 로그인 처리하기- 쿠키사용
#### 로그인 상태 유지하기
로그인 상태를 유지하려면 쿠키를 주로 사용한다.
#### 쿠키
서버에서 로그인에 성공하면 HTTP 응답에 쿠키를 담아서 브러우저에 전달한다. 그러면 브라우저는 앞으로 해당 쿠키를 지속해서 보내준다.

