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


## 로그인 처리하기 -서브릿 HTTP 세션2
     public String homeLoginV3Spring(
            @SessionAttribute(name=SessionConst.LOGIN_MEMBER,required = false)Member loginMember, Model model) {

        //세션에 회원 데이터가 없으면 home
        if (loginMember == null) {
            return "home";
        }
        //세션이 유지되면 로그인으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }
    
### @SessionAttribute
스프링은 세션을 더 편리하게 사용할 수 있도록 @SessionAttribute를 지원한다.
이미 로그인된 사용자를 찾을때는 다음과 같이 사용하면된다.
 > @SessionAttribute(name=SessionConst.LOGIN_MEMBER,required = false)Member loginMember

### TrackingMode
로그인을 처음 시도하면 URL이 다음과 같이 jsessionid 를 포함하고 있는 것을 확인할 수 있다.
> http://localhost:8080/;jsessionid=F59911518B921DF62D09F0DF8F83F872


이것은 웹 브라우저가 쿠키를 지원하지 않을 때 쿠키 대신 URL을 통해서 세션을 유지하는 방법이다. 이
방법을 사용하려면 URL에 이 값을 계속 포함해서 전달해야 한다. 타임리프 같은 템플릿은 엔진을 통해서
링크를 걸면 jsessionid 를 URL에 자동으로 포함해준다. 서버 입장에서 웹 브라우저가 쿠키를
지원하는지 하지 않는지 최초에는 판단하지 못하므로, 쿠키 값도 전달하고, URL에 jsessionid 도 함께
전달한다.
URL 전달 방식을 끄고 항상 쿠키를 통해서만 세션을 유지하고 싶으면 다음 옵션을 넣어주면 된다. 이렇게
하면 URL에 jsessionid 가 노출되지 않는다.
> ### application.properties  
> server.servlet.session.tracking-modes=cookie

### 세션 정보와 타임아웃 설정

* sessionId : 세션Id, JSESSIONID 의 값이다. 예) 34B14F008AA3527C9F8ED620EFD7A4E1
* maxInactiveInterval : 세션의 유효 시간, 예) 1800초, (30분)
* creationTime : 세션 생성일시
* lastAccessedTime : 세션과 연결된 사용자가 최근에 서버에 접근한 시간, 클라이언트에서 서버로
* sessionId ( JSESSIONID )를 요청한 경우에 갱신된다.
* isNew : 새로 생성된 세션인지, 아니면 이미 과거에 만들어졌고, 클라이언트에서 서버로
* sessionId ( JSESSIONID )를 요청해서 조회된 세션인지 여부

### 세션 타임아웃 설정
세션은 사용자가 로그아웃을 직접 호출해서 session.invalidate()가 호출 되는 경우에 삭제된다.
그런데 대부분의 사용자는 로그아웃을 선택하지 않고, 그냥 웹 브라우저를 종료한다. 문제는 HTTP가 비연결성(ConnectionLess)이므로 서버입장에서는 해당 사용자가 웹 브라우저를 종료한 것인지 아닌지 인식할 수 없다. 이 경우 남아있는 세션을 무한정 보관하면 다음과 같은 문제가 발생할 수 있다.
* 세션과 관련된 쿠키(JSESSIONID)를 탈취 당했을 경우 오랜 시간이 지나도 해당 쿠키로 악의적인 요청을 할 수 있다.
* 세션은 기본적으로 메모리에 생성된다. 메모리의 크기가 무한하지 않기 때문에 꼭 필요한 경우만 생성해서 사용해야한다.

### 세션의 종료 시점
세션의 종료 시점을 어떻게 정하면 좋을까? 가장 단순하게 생각해보면, 세션 생성 시점으로부터 30분
정도로 잡으면 될 것 같다. 그런데 문제는 30분이 지나면 세션이 삭제되기 때문에, 열심히 사이트를
돌아다니다가 또 로그인을 해서 세션을 생성해야 한다 그러니까 30분 마다 계속 로그인해야 하는
번거로움이 발생한다.
더 나은 대안은 세션 생성 시점이 아니라 사용자가 서버에 최근에 요청한 시간을 기준으로 30분 정도를
유지해주는 것이다. 이렇게 하면 사용자가 서비스를 사용하고 있으면, 세션의 생존 시간이 30분으로 계속
늘어나게 된다. 따라서 30분 마다 로그인해야 하는 번거로움이 사라진다. HttpSession 은 이 방식을
사용한다

### 정리
서블릿의 HttpSession 이 제공하는 타임아웃 기능 덕분에 세션을 안전하고 편리하게 사용할 수 있다. 
실무에서 주의할 점은 세션에는 최소한의 데이터만 보관해야 한다는 점이다. 보관한 데이터 용량 * 사용자
수로 세션의 메모리 사용량이 급격하게 늘어나서 장애로 이어질 수 있다. 추가로 세션의 시간을 너무 길게
가져가면 메모리 사용이 계속 누적 될 수 있으므로 적당한 시간을 선택하는 것이 필요하다. 기본이 30
분이라는 것을 기준으로 고민하면 된다.

## 서블릿 필터
### 공통 관심사항
요구사항을 보면 로그인 한 사용자만 상품 관리 페이지만 들어갈 수 있어야한다.
로그인을 하지 않은 사람에게는 상품 관리 버튼이 보이지 않기 때문에 문제가 없어보이지만. 로그인하지 않은 사람이 URL을 입력하면 상품관리 화면에 들어갈 수 있다.
상품 관리 컨트롤러에서 로그인 여부를 체크해서 로직을 하나하나 작성하면 되겠찌만 등록,수정,삭제,조회 등등 상품 관리 모든 컨트롤러 로직에 공통으로 로그인 여부를 체크하면되지만.
로그인과 관련된 로직을 수정할 때마다 모든 로직을 수정해야하는 큰 문제가 생긴다.
애플리케이션 여러 로직에서 공통으로 관심이 있는 것을 공통 관심사(cross-cutting concern)라고 한다. 여기서는 등록,수정,삭제,조회 등등 여러 로직에서 공통으로 인증에 대해서 관심을 가지고 있다.

공통 관심사는 스프링의 AOP로도 해결할 수 있지만, 웹과 관련된 공통 관심사는 서블릿 필터나, 스프링 인터셉터를 사용하는 것이 좋다. 웹과 관련된 공통 관심사를 처리할 때는 HTTP의 헤더나 URL의 정보들이 필요한데, 서블릿 필터나 스프링 인터셉터는 HttpServletRequest를 제공한다.
### 서블릿 필터 소개
필터는 서블릿이 지원하는 수문장이다. 필터의 특징
##### 필터의 흐름
> HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러

필터를 적용하면 필터가 호출된 다음 서블릿이 호출된다. 그래서 모든 고객의 요청 로그를 남기는 요구사항이 있다면 필터를 사용하면 된다. 
필터는 특정 URL패턴에 적용할 수 있다. /*이라고 하면 모든 요청에 필터가 적용된다.

#### 필터 제한
> HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 컨트롤러 //로그인 사용자
> HTTP 요청 -> WAS -> 필터(적절하지 않은 요청이라 판단, 서블릿 호출X) //비 로그인 사용자

필터에서 적용하지 않은 요청이라고 판단하면 거기에서 끝 낼수도 있다-> 로그인 여부를 체크하기 적합하다. 

#### 필터 체인
> HTTP 요청->WAS->필터1-> 필터2->필터3->서블릿->컨트롤러

필터는 체인으로 구성, 중간에 필터를 자유롭게 추가할 수 있다.

#### 필터 인터페이스
* init(): 필터 초기화 메서드, 서블릿 컨테이너가 생성될 떄 호출
* doFilter(): 고객의 요청이 올 때 마다 해당 메서드가 호출된다. 필터의 로직을 구현하면 된다.(중요)
* destroy(): 필터 종료 메서도, 서블릿 컨테이너가 종료될 때 호출


### 서블릿 필터-요청 로그
* public class LogFilter implements Filter {}  
필터를 사용하려면 필터 인터페이스를 구현해야 한다.
* doFilter(ServletRequest request, ServletResponse response, FilterChain chain)  
 HTTP 요청이 오면 doFilter 가 호출된다.
ServletRequest request 는 HTTP 요청이 아닌 경우까지 고려해서 만든 인터페이스이다. HTTP를
사용하면 HttpServletRequest httpRequest = (HttpServletRequest) request; 와 같이
다운 케스팅 하면 된다.
* String uuid = UUID.randomUUID().toString();  
HTTP 요청을 구분하기 위해 요청당 임의의 uuid 를 생성해둔다.
* log.info("REQUEST [{}][{}]", uuid, requestURI);  
uuid 와 requestURI 를 출력한다.
* chain.doFilter(request, response);  
이 부분이 가장 중요하다. 다음 필터가 있으면 필터를 호출하고, 필터가 없으면 서블릿을 호출한다. 
만약 이 로직을 호출하지 않으면 다음 단계로 진행되지 않는다.

필터를 등록하는 방법은 여러가지가 있지만, 스프링 부트를 사용한다면 FilterRegistrationBean 을
사용해서 등록하면 된다.

* setFilter(new LogFilter()) : 등록할 필터를 지정한다.
* setOrder(1) : 필터는 체인으로 동작한다. 따라서 순서가 필요하다. 낮을 수록 먼저 동작한다.
* addUrlPatterns("/*") : 필터를 적용할 URL 패턴을 지정한다. 한번에 여러 패턴을 지정할 수 있다

### 서블릿 필터 -인증 체크
로그인 되지 않은 사용자는 상품 관리 뿐만 아니라 미래에 개발될 페이지에도 접근하지 못하도록 만듦
* whitelist = {"/", "/members/add", "/login", "/logout","/css/*"};
   - 인증 필터를 적용해도 홈,회원가입,로그인 화면,css 같은 리소스에는 접근할 수 있도록 만든다. 이렇게 화이트 리스트 경로는 인증과 무관하게 항상 허용한다. 화이트 리스트를 제외한 모든 경로는 인증 체크 로직을 적용
* httpResponse.sendRedirect("/login?redirectURL=" + requestURI);
   - 미인증 사용자는 로그인 화면으로 리다이렉트, 그런데 로그인 이후에 다시 홈으로 이동해버리면 원하는 경로를 다시 찾아가야하는 불편함이 있따. 이런 부분은 개발자 입장에서는 좀 귀찮을 수 있어도 사용자 입장으로 보면 편리한 기능이다. 이러한 기능을 위해 현재 요청한 경로인 requestURI를 /login에 쿼리 파라미터로 함께 전달한다. 물론 /login컨트롤러에서 로그인 성공시 해당 경로로 이동하는 기능은 추가로 개발해야한다.

* return; 여기가 중요하다. 필터는 더는 진행하지 않는다. 이후 필터는 물론 서블릿,컨트롤러가 더는 호출 되지 않는다. 앞서 redirect를 사용했기 때문에 redirect가 응답으로 적용되고 요청이 끝난다.

* 로그인 체크 필터에서, 미인증 사용자는 요청 경로를 포함해서 /login 에 redirectURL 요청 파라미터를 추가해서 요청했다. 이 값을 사용해서 로그인 성공시 해당 경로로 고객을 redirect 한다.


### 스프링 인터셉터-소개
스프링 인터셉터도 서블릿 필터와 같이 웹과 관련된 공통 관심 사항을 효과적으로 해결한다.서블릿 필터가 서블릿이 제공하는 기술이라면, 스프링 인터셉터는 스프링 MVC가 제공하는 기술이다. 둘다
웹과 관련된 공통 관심 사항을 처리하지만, 적용되는 순서와 범위, 그리고 사용방법이 다르다
#### 스프링 인터셉터 흐름
HTTP 요청-> WAS -> (서블릿)필터-> (디스패처)서블릿->스프링 인터셉터 -> 컨트롤러
* 스프링 인터셉터는 디스패처 서블릿과 컨트롤러 사이에서 컨트롤러 호출 직전에 호출 된다.
* 스프링 인터셉터는 스프링 MVC가 제공하는 기능이기 때문에 결국 디스패처 서블릿 이후에 등장하게 된다. 
* 스프링 MVC의 시작점이 디스패처 서블릿이라고 생각해보면 이해가 될 것이다.
* 스프링 인터셉터에도 URL 패턴을 적용할 수 있는데, 서블릿 URL 패턴과는 다르고, 매우 정밀하게 설정할 수 있다

#### 스프링 인터셉터 제한
HTTP요청 -> WAS ->필터-> 서블릿 ->서블릿 인터셉터 -> 컨트롤러 //로그인 사용자  
HTTP요청 -> WAS ->필터-> 서블릿 -> 스프링인터셉터(적절하지 않은 요청이라 판단,컨트롤러 호출 X)//비 로그인 사용자
인터셉터에서 적절하지 않은 요청이라고 판단하면 거기에서 끝을 낼 수도 있다. 그래서 로그인 여부를 체크하기에 딱 좋다.

#### 스프링 인터셉터 체인
HTTP 요청 -> WAS -> 필터 -> 서블릿 -> 인터셉터1 -> 인터셉터2 -> 컨트롤러
스프링 인터셉터는 체인으로 구성되는데, 중간에 인터셉터를 자유롭게 추가할 수 있다. 예를 들어서 로그를 남기는 인터셉터를 먼저 적용하고, 그 다음에 로그인 여부를 체크하는 인터셉터를 만들 수 있다.

#### 스프링 인터셉터 인터페이스
* 서블릿 필터의 경우 단순하게 doFilter() 하나만 제공된다. 인터셉터는 컨트롤러 호출 전( preHandle ), 호출 후( postHandle ), 요청 완료 이후( afterCompletion )와 같이 단계적으로 잘 세분화 되어 있다.
* 서블릿 필터의 경우 단순히 request , response 만 제공했지만, 인터셉터는 어떤 컨트롤러( handler )가 호출되는지 호출 정보도 받을 수 있다. 그리고 어떤 modelAndView 가 반환되는지 응답 정보도 받을 수 있다.

#### 스프링 인터셉터 호출 흐름
##### 정상 흐름
* preHandle: 컨트롤러 호출 전에 호출된다 (더 정확히는 핸들러 어댑터 호출 전에 호출된다.)
   - preHandle의 응답값이 true이면 다음으로 진행하고 false이면 더는 진행하지 않는다. false인 경우 나머지 인터셉터는 물론이고 핸들러 어댑터도 호출되지 않는다. 
* postHandle: 컨트롤러 호출 후에 호출된다.
* afterCompletion: 뷰가 렌더링 된 이후에 호출된다.

#### 스프링 인터셉터 예외 상황
##### 예외발생시
* preHandler: 컨트롤러 호출 전에 호출
* postHandler: 컨트롤러에서 예외가 발생하면 postHandler은 호출되지 않는다.
* afterCompletion : afterCompletion 은 항상 호출된다. 이 경우 예외( ex )를 파라미터로 받아서 어떤 예외가 발생했는지 로그로 출력할 수 있다.

##### afterCompletion은 예외가 발생해도 호출된다.
* 예외가 발생하면 postHandle() 는 호출되지 않으므로 예외와 무관하게 공통 처리를 하려면 afterCompletion() 을 사용해야 한다.
* 예외가 발생하면 afterCompletion() 에 예외 정보( ex )를 포함해서 호출된다.

##### 정리
인터셉터는 스프링 MVC 구조에 특화된 필터 기능을 제공한다고 이해하면 된다. 스프링 MVC를 사용하고,  특별히 필터를 꼭 사용해야 하는 상황이 아니라면 인터셉터를 사용하는 것이 더 편리하다.

## 스프링 인터셉터 -요청 로그
* String uuid = UUID.randomUUID().toString()
   - 요청 로그를 구분하기 위한 uuid 를 생성한다.
* request.setAttribute(LOG_ID, uuid)
  - 서블릿 필터의 경우 지역변수로 해결이 가능하지만, 스프링 인터셉터는 호출 시점이 완전히 분리되어
있다. 따라서 preHandle 에서 지정한 값을 postHandle , afterCompletion 에서 함께 사용하려면
어딘가에 담아두어야 한다. LogInterceptor 도 싱글톤 처럼 사용되기 때문에 맴버변수를 사용하면
위험하다. 따라서 request 에 담아두었다. 이 값은 afterCompletion 에서 request.getAttribute(LOG_ID) 로 찾아서 사용한다.
* return true
  - true 면 정상 호출이다. 다음 인터셉터나 컨트롤러가 호출된다

#### HandlerMethod
핸들러 정보는 어떤 핸들러 매핑을 사용하는가에 따라 달라진다. 스프링을 사용하면 일반적으로  @Controller,@RequestMapping을 활용한 핸들러 매핑을 사용하는데, 이 경우 핸들러 정보로 HandlerMethod가 넘어온다.

#### ResourceHttpRequestHandler
@Controller가 아니라 /resources/static와 같은 정적 리소스가 호출 되는 경우
RequestHttpRequestHandler가 핸들러 정보로 넘어오기 때문에 타입에 따라서 처리가 필요하다.
#### postHandle, afterCompletion
종료 로그를 postHandler이 아니라 afterCompletion에서 실행한 이유는, 예외가 발생한 경우 postHandle가 호출되지 않기 때문이다. afterCompletion은 예외가 발생해도 호출 되는 것을 보장한다.
#### WebConfig-인터셉터 등록
인터셉터와 필터가 중복되지 않도록 필터를 등록하기 위한 logFilter() 의 @Bean 은 주석처리
WebMvcConfigurer 가 제공하는 addInterceptors() 를 사용해서 인터셉터를 등록할 수 있다.
* registry.addInterceptor(new LogInterceptor()) : 인터셉터를 등록한다.
* order(1) : 인터셉터의 호출 순서를 지정한다. 낮을 수록 먼저 호출된다.
* addPathPatterns("/**") : 인터셉터를 적용할 URL 패턴을 지정한다.
* excludePathPatterns("/css/**", "/*.ico", "/error") : 인터셉터에서 제외할 패턴을 지정한다.

필터와 비교해보면 인터셉터는 addPathPatterns , excludePathPatterns 로 매우 정밀하게 URL 
패턴을 지정할 수 있다

## 스프링 인터셉터 -인증 체크
서블릿 필터에서 사용했던 인증 체크 기능을 스프링 인터셉터로 개발
#### LoginCheckInterceptor
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        log.info("인증 체크 인터셉터 실행{}",requestURI);

        HttpSession session=request.getSession();

        if(session ==null||session.getAttribute(SessionConst.LOGIN_MEMBER)==null){
            log.info("미인증 사용자 요청");
            //로그인으로 redirect
            response.sendRedirect("/login?redirectURL="+requestURI);
            return false;
        }

        return true;
    }
    
서블릿 필터와 비교해서 코드가 매우 간결하다. 인증이라는 것은 컨트롤러 호출 전에만 호출되면 된다. 
따라서 preHandle 만 구현하면 된다.


#### 순서 주의 ,세밀한 설정 가능
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor())
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/***","/*.ico","/errors");

        registry.addInterceptor(new LoginCheckInterceptor())
                .order(2)
                .addPathPatterns("/**")
                .excludePathPatterns("/","/members/add","/login","/logout","/css/**","/*.ico","/error");
    }
    
    
인터셉터와 필터가 중복되지 않도록 필터를 등록하기 위한 logFilter() , loginCheckFilter() 의 @Bean 은 주석처리하자.
인터셉터를 적용하거나 하지 않을 부분은 addPathPatterns와 excludePathPatterns에 작성하면된다.
기본적으로 모든 경로에 해당 인터셉터를 적용하되 (/**),홈(/),회원가입(/members/add),로그인(/login),리소스조회(/css/**),오류(/error)와 같은 부분은 로그인 체크 인터셉터를 적용하지 않는다. 서블릿 필터와 비교해보면 매우 편리한 것을 알 수 있다.

##### 정리
서블릿 필터와 스프링 인터셉터는 웹과 관련된 공통 관심사를 해결하기 위한 기술이다.
서블릿 필터와 비교해서 스프링 인터셉터가 개발자 입장에서 훨씬 편리하다는 것을 코드로 이해했을 것이다. 특별한 문제가 없다면 인터셉터를 사용하는 것이 좋다

## ArgumentResolver 활용
ArgumentResolver 를 학습했다. 이번 시간에는 해당 기능을 사용해서 로그인 회원을 조금 편리하게 만들어본다.

### HomeController 수정
    @GetMapping("/")
    public String homeLoginV3ArgumentResolver(@Login Member loginMember, Model model) {

        //세션에 회원 데이터가 없으면 home
        if (loginMember == null) {
            return "home";
        }
        //세션이 유지되면 로그인으로 이동
        model.addAttribute("member", loginMember);
        return "loginHome";
    }

@Login 애노테이션이 있으면 직접 만든 ArgumentResolver가 동작해서 자동으로 세션에 있는 로그인 회원을 찾아주고 만약 세션에 없다면  null을 반환하도록 개발
### @Login 애노테이션 생성
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Login {
    }
* @Target(ElementType.PARAMETER) : 파라미터에만 사용
* @Retention(RetentionPolicy.RUNTIME) : 리플렉션 등을 활용할 수 있도록 런타임까지 애노테이션 정보가 남아있음    

### LoginMemberArgumentResolver 생성
    @Slf4j
    public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        log.info("supportsParameter 실행");

        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        return hasLoginAnnotation&&hasMemberType;   //둘다 true이면 resolveArgument가 실행

    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        log.info("resolveArgument 실행");

        HttpServletRequest request =(HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if(session==null){
            return null;
        }

        return session.getAttribute(SessionConst.LOGIN_MEMBER);
    }
    }
    
* supportsParameter() : @Login 애노테이션이 있으면서 Member 타입이면 해당 ArgumentResolver 가 사용된다.
* resolveArgument() : 컨트롤러 호출 직전에 호출 되어서 필요한 파라미터 정보를 생성해준다. 여기서는 세션에 있는 로그인 회원 정보인 member 객체를 찾아서 반환해준다. 이후 스프링MVC는 컨트롤러의 메서드를 호출하면서 여기에서 반환된 member 객체를 파라미터에 전달해준다 
  
 ### WebConfig에 추가   
    //ArgumentResolver등록
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
       resolvers.add(new LoginMemberArgumentResolver());
    }
    
 앞서 개발한 LoginMemberArgumentResolver를 등록
 ### 실행
실행해보면, 결과는 동일하지만, 더 편리하게 로그인 회원 정보를 조회할 수 있다. 이렇게
ArgumentResolver 를 활용하면 공통 작업이 필요할 때 컨트롤러를 더욱 편리하게 사용할 수 있다.
