package taskagile.domain.application.impl;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import taskagile.domain.application.UserService;
import taskagile.domain.application.command.RegistrationCommand;
import taskagile.domain.common.event.DomainEventPublisher;
import taskagile.domain.common.mail.MailManager;
import taskagile.domain.common.mail.MessageVariable;
import taskagile.domain.model.user.RegistrationException;
import taskagile.domain.model.user.RegistrationManagement;
import taskagile.domain.model.user.User;
import taskagile.domain.model.user.event.UserRegisteredEvent;


// 간단한 CRUD 애플리케이션에서는 web, service, domain, dao 등으로 나누는 계층형 아키텍처가 보기 편하고 좋지만,
// 예를 들어 같은 User 도메인에 속하는 클래스가 service 패키지에 들어가기도하고, domain 패키지에 들어가기도한다.

// 그래서 이 애플리케이션에서는 헥사고날 아키텍처 (육각형 아키텍처) 를 채택하여, 
// 하나의 도메인에 속하는 모든 클래스를 같은 도메인에 놓는다. (User 도메인에 속하는 모든 클래스 같은 패키지에 놓는다)


// 도메인 주도 설계를 따르는 애플리케이션에서 서비스는 애플리케이션 서비스, 도메인 서비스, 인프라 서비스 세 종류로 구분된다.

// 이 애플리케이션의 패키지들의 이름을 보면, 
// 컨트롤러, 페이로드, 응답코드 등은 taskagile.web으로 시작
// 애플리케이션 서비스는 taskagile.domain.application으로 시작
// 도메인 서비스는 taskagile.domain.model로 시작
// 인프라 서비스는 taskagile.domain.common으로 시작한다.


// 애플리케이션 서비스 : 예를 들어 이 클래스, 사용자를 어떻게 등록하는지 관여 X (등록은 도메인 서비스인 RegistrationManagement에 의존)
// 스프링에서 @Service 애노테이션은 대부분 어플리케이션 서비스에 적용 / 일반적으로 생각하는 서비스, 즉 컨트롤러에 기능을 제공하는 서비스는 애플리케이션 서비스

// 이 UserService는 모델의 작업을 조정, 비즈니스 로직을 수용 X
// 클라이언트가 도메인 모델에 접근하는 것을 방지 / 트랜잭션을 컨트롤, @Transactional 


// 도메인 서비스 : 예를 들어 RegistrationManagement, 도메인 객체에 자연스럽게 어울리지 못하는 비즈니스 로직을 캡슐화,
// 여기에서 말하는 비즈니스 로직은 리포지토리에 포함되는 일반적인 CRUD 연산이 아니다. / 이 서비스에서 사용자 등록을 처리한다.

// 인프라 서비스 : 예를 들어 UserRepository, 메일 서비스, 도메인 이벤트 발신 / 이들은 일반적으로 메일 서버, 데이터베이스, 메시징 큐, 캐시 서버, 서드파티 REST API 등 외부 리소스를 활용
// 이들은 도메인 모델의 주요 문제에 해당하지 않는다.

@Service  		// 이 어노테이션이 적용된 클래스는 오직 해당 클래스를 활용하는 클라이언트를 위한 연산을 제공한다는 의미
@Transactional
public class UserServiceImpl implements UserService {

  private RegistrationManagement registrationManagement;  // 도메인 서비스 
  private DomainEventPublisher domainEventPublisher;      // 인프라 서비스
  private MailManager mailManager;						  // 인프라 서비스

  public UserServiceImpl(RegistrationManagement registrationManagement, DomainEventPublisher domainEventPublisher, MailManager mailManager) {
    this.registrationManagement = registrationManagement;
    this.domainEventPublisher = domainEventPublisher;
    this.mailManager = mailManager;
  }

  // UserService 구현체가 구현해야하는 메서드에서는 도메인 서비스와 인프라 서비스에 의존하여, 회원가입이나, 메일 발송, 도메인 이벤트 발송 등을 제공한다.
  // UserService는 어떤 비즈니스 로직도 포함하지 않는다.
  @Override	
  public void register(RegistrationCommand command) throws RegistrationException {
	// command가 null이면, Assert.notNull() 메서드가 IllegalArgumentException 에러를 던진다. (notNull - 널이 아니어야한다라는 검사 구문)
    Assert.notNull(command, "Parameter `command` must not be null"); 
    
    // 이 이후에 더는 RegistrationCommand 인스턴스를 매개변수로 넘기지 않는다. RegistrationCommand는 컨트롤러와 같은 애플리케이션 서비스의 클라이언트가 활용하는 것이기 때문이다.
    // 애플리케이션 코어에서는 , 코어의 내부가 외부와 분리될 수 있도록 RegistrationCommand를 사용하지 않는다.
    User newUser = registrationManagement.register(command.getUsername(),command.getEmailAddress(),command.getPassword());

    sendWelcomeMessage(newUser); 
    domainEventPublisher.publish(new UserRegisteredEvent(newUser));
  }

  private void sendWelcomeMessage(User user) { 
    mailManager.send(
      user.getEmailAddress(),
      "Welcome to TaskAgile",
      "welcome.ftl",
      MessageVariable.from("user", user)
    );
  }
}