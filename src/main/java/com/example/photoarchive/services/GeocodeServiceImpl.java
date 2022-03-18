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
import java.util.Objects;
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
		var storage = new Object() {
			String country = "", locality = "", address = "";
			String localityFromRoute = "";
			String route = "";
		};

//        log.debug(geocode);

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseJsonNode = mapper.readTree(geocode);

			var status = getText(responseJsonNode, "status");

			if (status.equalsIgnoreCase("ok")) {
				storage.localityFromRoute = "";
				var results = responseJsonNode.get("results");
				if (Objects.nonNull(results))
					results.elements().forEachRemaining(elm -> {
						storage.route = "";
						if (typeIs(elm, "route")) {
							var addressComponents = elm.get("address_components");
							if (Objects.nonNull(addressComponents))
								addressComponents.elements().forEachRemaining(component -> {
									//Console.WriteLine(elmr.ToString()); Console.WriteLine(); Console.WriteLine("route before {0}", route);
									String long_name = getText(component, "long_name");
									if (typeIs(component, "route") && long_name.equalsIgnoreCase("Unnamed Road"))
										storage.route += (storage.route.isEmpty() ? "" : ", ") + long_name;
									if (typeIs(component, "political")
											&& !typeIs(component, "administrative_area_level_2")
											&& !typeIs(component, "administrative_area_level_3")
											&& !typeIs(component, "administrative_area_level_4")
											&& !typeIs(component, "administrative_area_level_5"))
										storage.route += (storage.route.isEmpty() ? "" : ", ") + long_name;
									if (typeIs(component, "locality"))
										storage.localityFromRoute += (storage.localityFromRoute.isEmpty() ? "" : ", ") + long_name;
									//Console.WriteLine("route after {0}", route); Console.WriteLine();
								});
						}
						//Console.WriteLine("localityFromRoute {0}", localityFromRoute); Console.WriteLine();
						//Console.WriteLine("locality {0}", locality); Console.WriteLine();
						String formatted_address = getText(elm, "formatted_address");

						if (typeIs(elm, "country")) storage.country = formatted_address;
						if (typeIs(elm, "locality")) storage.locality = formatted_address;
						if (storage.locality.isEmpty())
							if (typeIs(elm, "administrative_area_level_2"))
								storage.locality = storage.localityFromRoute + (storage.localityFromRoute.isEmpty() ? "" : ", ") + formatted_address;
						if (typeIs(elm, "street_address"))
							storage.address = formatted_address;
						if (storage.address.isEmpty()) {
							if (typeIs(elm, "route"))
								storage.address = storage.route;
							//Console.WriteLine("route standart {0}", formatted_address);
							//Console.WriteLine("route my       {0}", route);
						}

					});
			} else { // response != OK
				log.error("Error from Google {}. No Address Found", status);
				return null;
			}
		} catch (JsonProcessingException e) {
			log.error(e);
			return null;
		}
//        log.debug("country:{} locality:{} address:{}", storage.country, storage.locality, storage.address);
		return ReadableGeoInfo.builder()
				.country(storage.country)
				.locality(storage.locality)
				.address(storage.address)
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

//    private void printResponse(String response) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode responseJsonNode = mapper.readTree(response);
//            printJsonNode(responseJsonNode, "root", 0);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void printJsonNode(JsonNode node, String name, int level) {
//        if (node.isValueNode()) {
//            Serializable value = nodeValue(node);
//            log.debug("{}{}{}", " ".repeat(level), name + (name.isEmpty() ? "" : " : "), value);
//        }
//        if (node.isArray()) {
//            log.debug("{}{} : [", " ".repeat(level), name);
//            Iterator<JsonNode> elements = node.elements();
//            elements.forEachRemaining(e -> printJsonNode(e, "", level + 2));
//            log.debug("{}]", " ".repeat(level));
//        }
//        if (node.isObject()) {
//            log.debug("{}{}{", " ".repeat(level), name + (name.isEmpty() ? "" : " : "));
//            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
//            fields.forEachRemaining(e -> printJsonNode(e.getValue(), e.getKey(), level + 2));
//            log.debug("{}}", " ".repeat(level));
//        }
//    }

//    private Serializable nodeValue(JsonNode node) {
//        try {
//            return node.isBinary() ? node.binaryValue()
//                    : node.isBigDecimal() ? node.decimalValue()
//                    : node.isBigInteger() ? node.bigIntegerValue()
//                    : node.isBoolean() ? node.booleanValue()
//                    : node.isDouble() ? node.doubleValue()
//                    : node.isFloat() ? node.floatValue()
//                    : node.isFloatingPointNumber() ? node.isFloatingPointNumber()
//                    : node.isInt() ? node.intValue()
//                    : node.isLong() ? node.longValue()
//                    : node.isNumber() ? node.numberValue()
//                    : node.isShort() ? node.shortValue()
//                    : node.isTextual() ? node.textValue()
//                    : "";
//        } catch (IOException ignored) {
//        }
//        return "";
//    }
}
