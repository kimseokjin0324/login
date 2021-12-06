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
로그인 시 쿠키를 이용해 로그인 정보나 중요한 정보들을 저장하면 탈취해서 사용할 수 있는 문제가 있다
-> 세션을 사용해서 서버에서 중요한 정보를 관리한다.
* 쿠키 값을 변조가능하다는 문제를 예상 불가능한 복잡한 세션 ID를 사용해서 해결할 수 있따.
* 쿠키에 보관하는 정보는 클라이언트 해킹시 털릴 가능성이 있지만 세션 ID는 랜덤한 값이고 중요한 정보가 없다.
* 쿠키는 탈취 후 사용가능하지만 해커가 토큰을 털어가도 시간이 지나면 사용할 수 없도록 세션의 만료시간을 짧게(예:30분)으로 유지한다. 또는 해킹이 의심되는 경우 서버에서 해당 세션을 강제로 제거하면된다. 
### 세션 직접 만들기
#### 세션 생성
* sessionId 생성(임의의 추정 불가능한 랜던 값)
* 세션 저장소에 sessionId와 보관할 값 저장
* sessionId로 응답쿠키를 생성해서 클라이언트에 전달
#### 세션 조회
* 클라이언트가 요청한 sessionId 쿠키의 값으로,세션 저장소에 보관한 값 조회
#### 세션 만료
* 클라이언트가 요청한 sessionId 쿠키의 값으로,세션 저장소에 보관한 sessionId와 값 제거
세션관리자에서 저장된 회원 정보를 조회한다. 만약 회원 정보가 없으면, 쿠키나 세션이 없는 것 이므로 로그인 되지 않은 것으로 처리한다.
#### 정리
세션과 쿠키의 개념을 명확히 이해하기 위해서 세션메니저를 만들었다. 세션은 뭔가 특별한 것이 아니라 쿠키를 사용하는데, 서버에서 데이터를 유지하는 방법일 뿐이라는것을 이해하면된다.
그런데 프로젝트마다 이런 세션 개념을 직접 개발하는건 불편하다. 그래서 서블릿도 세션 개념을 지원한다.서블릿이 공식 지원하는 세션은 우리가 직접 만든 세션과 동작 방식이 거의 같다. 
추가로 세션을 이정기간 사용하지 않으면 해당 세션을 삭제하는 기능을 제공


#### HttpSession소개
서블릿이 제공하는 HttpSession도 결국 직접만든 SessionManager와 같은 방식으로 동작
서블릿을 통해 HttpSession을 생성하면 다음과같은 쿠키를 생성, 쿠키이름이 JSESSIONID이고 값은 추정 불가능한 랜덤값이다.
#### 세션 생성과 조회
세션을 생성하려면 request.getSession(true) 를 사용하면 된다.
> public HttpSession getSession(boolean create);
#### 세션의 create 옵션에 대해 알아보자.
* request.getSession(true)
   - 세션이 있으면 기존 세션을 반환한다.
   - 세션이 없으면 새로운 세션을 생성해서 반환한다.
* request.getSession(false)
   - 세션이 있으면 기존 세션을 반환한다.
   - 세션이 없으면 새로운 세션을 생성하지 않는다. null 을 반환한다.
