package com.example.photoarchive.components;

import com.example.photoarchive.experiment.DialogNoPadding;
import com.example.photoarchive.security.AuthenticatedUser;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

@Log4j2
public class MainAppLayout extends AppLayout implements LocaleChangeObserver, BeforeEnterObserver {
	private final AuthenticatedUser authenticatedUser;
	private final AccessAnnotationChecker accessChecker;

	private final HorizontalLayout header = new HorizontalLayout();
	private final VerticalLayout drawer = new VerticalLayout();
	H1 titleName = new H1();
	private String urlOnStart;

	public MainAppLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker) {
		this.authenticatedUser = authenticatedUser;
		this.accessChecker = accessChecker;
		setPrimarySection(Section.DRAWER);      // program name + menu must be on left, burger + name view component + logout on right
		addToNavbar(header);
		addToDrawer(drawer);
		loadLayout();
	}

	private void loadLayout() {
		fillHeader();
		fillDrawer();
	}

	private void fillHeader() {
		DrawerToggle drawerToggle = new DrawerToggle();
		Button button = new Button();
		if (authenticatedUser.get().isPresent()) {
			button.setText(getTranslation("Logout"));
			button.setIcon(VaadinIcon.SIGN_OUT.create());
			button.addClickListener(e -> new LogoffDialogComponent(authenticatedUser::logout));
		} else {
			button.setText(getTranslation("Login"));
			button.setIcon(VaadinIcon.SIGN_IN.create());
			button.addClickListener(e -> UI.getCurrent().navigate(LoginView.class));
		}
		button.getStyle().set("margin", "0 .5em 0 0").set("flex-shrink", "0");

		titleName.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

		header.removeAll();
		header.add(drawerToggle);
		header.add(titleName);
		header.add(button);

		header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
		header.expand(titleName);
		header.setWidthFull();
	}

	private void fillDrawer() {
		HorizontalLayout logoPlusName = new HorizontalLayout() {{
			add(new Image("https://icons.iconarchive.com/icons/thiago-silva/palm/128/Photos-icon.png", "Logo") {{
				setWidth("50px");
			}});
			add(new H1(getTranslation("Program_name")) {{
				getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
			}});
		}};

//		"https://icons.iconarchive.com/icons/designcontest/ecommerce-business/128/photos-icon.png"
//		"https://icons.iconarchive.com/icons/thiago-silva/palm/128/Photos-icon.png"

		drawer.removeAll();
		drawer.add(logoPlusName);
		drawer.add(fillTabs());
		drawer.add(footer());
	}

//	private void callConsumerOnLoad1(@NonNull Consumer<StreamResource> consumer, @NonNull CompletableFuture<byte[]> data) {
//		data.thenAcceptAsync(d -> consumer.accept(new StreamResource("src", () -> new ByteArrayInputStream(d))));
//	}

//	private CompletableFuture<byte[]> readPictureDataAsync(String uri) {
//		return CompletableFuture.supplyAsync(() -> {
//			try (FileInputStream inputStream = new FileInputStream(uri)) {
//				return IOUtils.toByteArray(inputStream);
//			} catch (IOException e) {
//				log.error("Can't read data {}", uri);
//				return null;
//			}
//		});
//	}

	private void makeTab(VaadinIcon viewIcon, String viewName, Class<? extends Component> routeTo, Tabs tabs) {
		if (accessChecker.hasAccess(routeTo)) {
			Icon icon = iconForSpan(viewIcon);
			RouterLink link = new RouterLink();
			link.setRoute(routeTo);
			link.add(icon, new Span(viewName));
			var tab = new Tab(link);
			tabs.add(tab);
			if (Stream.of(
							Stream.of(routeTo.getAnnotationsByType(Route.class)).map(Route::value),
							Stream.of(routeTo.getAnnotationsByType(RouteAlias.class)).map(RouteAlias::value)
					).flatMap(a -> a)
					.anyMatch(s -> s.equalsIgnoreCase(urlOnStart))
			) tabs.setSelectedTab(tab);
		}
	}

	private Tabs fillTabs() {
		Tabs tabs = new Tabs();
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
		makeTab(VaadinIcon.INFO_CIRCLE, getTranslation("About"), AboutView.class, tabs);
		makeTab(VaadinIcon.DROP, getTranslation("Drop"), DropView.class, tabs);
		makeTab(VaadinIcon.DASHBOARD, getTranslation("Dashboard"), DashboardView.class, tabs);
		makeTab(VaadinIcon.FILE, getTranslation("Files"), FilesView.class, tabs);
		makeTab(VaadinIcon.COGS, getTranslation("Setting"), SettingView.class, tabs);
		makeTab(VaadinIcon.PLAY_CIRCLE, getTranslation("SlideShow"), SlideShow.class, tabs);
		makeTab(VaadinIcon.STETHOSCOPE, "dialog-no-padding", DialogNoPadding.class, tabs);
		return tabs;
	}

	private Footer footer() {
		Footer layout = new Footer();
//        layout.addClassNames("flex", "items-center", "my-s", "px-m", "py-xs");
		authenticatedUser.get()
				.map(u -> new Span(u.getUsername()))
				.ifPresent(c -> layout.add(iconForSpan(VaadinIcon.USER), c));
		return layout;
	}

	private Icon iconForSpan(VaadinIcon viewIcon) {
		Icon icon = viewIcon.create();
		icon.getStyle()
				.set("box-sizing", "border-box")
				.set("margin-inline-end", "var(--lumo-space-m)")
				.set("margin-inline-start", "var(--lumo-space-xs)")
				.set("padding", "var(--lumo-space-xs)");
		return icon;
	}

	private String getCurrentPageTitle() {
		var component = getContent().getClass();
		PageTitle title = component.getAnnotation(PageTitle.class);
		return getTranslation(title == null ? component.getSimpleName().replaceAll("View$", "") : title.value());
	}

	@Override
	public void localeChange(LocaleChangeEvent localeChangeEvent) {
		loadLayout();
		afterNavigation();
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		titleName.setText(getCurrentPageTitle());
	}

	@Override
	public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
		urlOnStart = beforeEnterEvent.getLocation().getPath();
//		log.debug(urlOnStart);
	}
}
