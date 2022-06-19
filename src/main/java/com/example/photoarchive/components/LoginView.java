package com.example.photoarchive.components;

import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.router.Route;

@Route("login")
public class LoginView extends LoginOverlay {
	public LoginView() {
		setAction("login");
		LoginI18n i18n = LoginI18n.createDefault();
		i18n.setHeader(new LoginI18n.Header());
		i18n.getHeader().setTitle("Photo-Archive");
		i18n.getHeader().setDescription("Login using user/user or admin/admin");
		i18n.setAdditionalInformation("Additional information");
		setI18n(i18n);

		setForgotPasswordButtonVisible(false);
		setOpened(true);
	}
}

//public class LoginView extends VerticalLayout implements BeforeEnterObserver {
//    private final LoginForm login = new LoginForm();
//
//    public LoginView() {
//        addClassName("login-view");
//        setSizeFull();
//        setAlignItems(Alignment.CENTER);
//        setJustifyContentMode(JustifyContentMode.CENTER);
//        login.setAction("login");
//        add(new H2("Photo Archive"), login);
//
//    }
//
//    @Override
//    public void beforeEnter(BeforeEnterEvent event) {
//        var isErrors = event.getLocation()
//                .getQueryParameters()
//                .getParameters()
//                .containsKey("error");
//        if (isErrors)
//            login.setError(isErrors);
//        else {
//            var intendedPath = Optional.ofNullable(VaadinSession.getCurrent().getAttribute("intendedPath"))
//                    .map(Object::toString)
//                    .orElse("");
//            UI.getCurrent().navigate(intendedPath);
//        }
//    }
//
//}
