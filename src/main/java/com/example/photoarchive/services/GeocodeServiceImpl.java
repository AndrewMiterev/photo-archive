package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.GeoCache;
import com.example.photoarchive.domain.entities.ReadableGeoInfo;
import com.example.photoarchive.domain.repo.GeoCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Service
public class GeocodeServiceImpl implements GeocodeService {
	private final ConfigProperties config;
	private final GeoCacheRepository repository;

	private static final String GOOGLE_URL_TEMPLATE = "https://maps.googleapis.com/maps/api/geocode/json?key=%s&latlng=%s,%s&language=%s";

	public GeocodeServiceImpl(ConfigProperties config, GeoCacheRepository repository) {
		this.config = config;
		this.repository = repository;
	}

	private String getGeocode(double latitude, double longitude) {
		String url = GOOGLE_URL_TEMPLATE.formatted(config.getGoogleApiKey(), latitude, longitude, config.getDefaultLanguage());
		log.debug("url {}", url);

		try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(url);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			return httpclient.execute(request, responseHandler);
		} catch (IOException e) {
			log.error(e);
			throw new RuntimeException(e);
		}
	}

	private boolean typeIs(JsonNode node, String name) {
		AtomicBoolean found = new AtomicBoolean(false);
		node.fields().forEachRemaining(f -> {
			if (f.getKey().equalsIgnoreCase("types")) {
				f.getValue().elements().forEachRemaining(e -> {
					if (e.textValue().equalsIgnoreCase(name))
						found.set(true);
				});
			}
		});
		return found.get();
	}

	private String getText(JsonNode node, String name) {
		var jsonNode = node.get(name);
		if (Objects.isNull(jsonNode)) return "";
		return jsonNode.textValue();
	}

	@Override
	public ReadableGeoInfo resolve(String geocode) {
		Set<String> country = new LinkedHashSet<>();
		Set<String> admin1 = new LinkedHashSet<>();
		Set<String> admin2 = new LinkedHashSet<>();
		Set<String> admin3 = new LinkedHashSet<>();
		Set<String> admin4 = new LinkedHashSet<>();
		Set<String> admin5 = new LinkedHashSet<>();
		Set<String> politicalLocality = new LinkedHashSet<>();
		Set<String> routeLocality = new LinkedHashSet<>();
		Set<String> plusLocality = new LinkedHashSet<>();
		Set<String> address = new LinkedHashSet<>();
		Set<String> route = new LinkedHashSet<>();
		Set<String> poi = new LinkedHashSet<>();

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJsonNode = mapper.readTree(geocode);

			var status = getText(responseJsonNode, "status");

			if (status.equalsIgnoreCase("ok")) {
				var results = responseJsonNode.get("results");
				if (Objects.nonNull(results)) results.elements().forEachRemaining(element -> {
					var addressComponents = element.get("address_components");
					if (Objects.nonNull(addressComponents)) addressComponents.elements().forEachRemaining(component -> {
						String long_name = getText(component, "long_name");
						if (typeIs(component, "country")) country.add(long_name);
						if (typeIs(component, "administrative_area_level_1")) admin1.add(long_name);
						if (typeIs(component, "administrative_area_level_2")) admin2.add(long_name);
						if (typeIs(component, "administrative_area_level_3")) admin3.add(long_name);
						if (typeIs(component, "administrative_area_level_4")) admin4.add(long_name);
						if (typeIs(component, "administrative_area_level_5")) admin5.add(long_name);
						if (typeIs(component, "political"))
							if (typeIs(component, "locality")) politicalLocality.add(long_name);
						if (typeIs(component, "route"))
							if (typeIs(component, "locality")) routeLocality.add(long_name);
						if (typeIs(component, "plus_code"))
							if (typeIs(component, "locality")) plusLocality.add(long_name);
						if (typeIs(component, "point_of_interest"))
							poi.add(long_name);
					});
					if (typeIs(element, "street_address")) {
						String formatted_address = getText(element, "formatted_address");
						Set<String> postalCode = new HashSet<>();
						var addressComponents1 = element.get("address_components");
						if (Objects.nonNull(addressComponents1))
							addressComponents1.elements().forEachRemaining(element1 -> {
								if (typeIs(element1, "postal_code"))
									postalCode.add(getText(element1, "long_name"));
							});
						var post = postalCode.stream().findFirst();
						if (post.isPresent()) {
							formatted_address = formatted_address.replace(", %s".formatted(post.get()), "");
							formatted_address = formatted_address.replace(post.get(), "");
						}
						address.add(formatted_address);
					}
					if (typeIs(element, "route")) if (Objects.nonNull(addressComponents))
						addressComponents.elements().forEachRemaining(component -> {
							String long_name = getText(component, "long_name");
							if (!(
									long_name.equalsIgnoreCase("Unnamed Road") ||                // english
											long_name.equalsIgnoreCase("Väg utan namn") ||       // finish
											long_name.equalsIgnoreCase("Nimetön tie") ||         // swedish
											long_name.equalsIgnoreCase("Нeizvestnaya doroga")    // russian
											|| typeIs(component, "postal_code")
							)) {
								route.add(long_name);
							}
						});
				});
			} else { // response != OK
				log.error("Error from Google {}. No Address Found", status);
				return null;
			}
		} catch (JsonProcessingException e) {
			log.error(e);
			return null;
		}

		Set<String> localities = new LinkedHashSet<>();
		localities.addAll(plusLocality);
		localities.addAll(routeLocality);
		localities.addAll(politicalLocality);
		localities.addAll(admin5);
		localities.addAll(admin4);
		localities.addAll(admin3);
		localities.addAll(admin2);
		localities.addAll(admin1);
		localities.addAll(country);
		if (localities.isEmpty()) localities.addAll(route);		// at least fill it with something from route

		Set<String> addresses = new LinkedHashSet<>();
		if (address.isEmpty()) addresses.addAll(route);
		addresses.addAll(address.stream().findFirst().stream().toList());
		if (addresses.isEmpty()) addresses.addAll(localities);		// at least fill it with something

		return ReadableGeoInfo.builder()
				.country(String.join(", ", country))
				.locality(String.join(", ", localities))
				.address(String.join(", ", addresses))
				.poi(String.join(", ", poi))
				.build();
	}

	@Override
	public String status(String geocode) {
		try {
			return getText(new ObjectMapper().readTree(geocode), "status");
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public String get(Geo geo) {
		var cache = repository.findById(geo);
		if (cache.isPresent()) return cache.get().getGeoCode();
		return update(geo);
	}

	@Override
	public String update(Geo geo) {
		var geoCode = pack(getGeocode(geo.getLatitude(), geo.getLongitude()));
		repository.save(GeoCache.builder().geo(geo).geoCode(geoCode).build());
		return geoCode;
	}

	private String pack(String geocode) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJsonNode = mapper.readTree(geocode);
			return responseJsonNode.toString();
		} catch (JsonProcessingException e) {
			log.error(e);
			return geocode;
		}
	}
}
