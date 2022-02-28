package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Geo;
import com.example.photoarchive.domain.entities.GeoCache;
import com.example.photoarchive.domain.entities.ReadableGeocode;
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
	public ReadableGeocode resolve(String geocode) {
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
		return ReadableGeocode.builder()
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
		var geoCode = getGeocode(geo.getLatitude(), geo.getLongitude());
		repository.save(GeoCache.builder().geo(geo).geoCode(geoCode).build());
		return geoCode;
	}

	@Override
	public String pack(String geocode) {
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

	private String getDemoGeocodeJson() {
		return """
				                    {
				                    "plus_code" : {
				                "compound_code" : "XR85+G6W Rishon LeTsiyon, Israel",
				                        "global_code" : "8G3PXR85+G6W"
				            },
				            "results" : [
				            {
				                "address_components" : [
				                {
				                    "long_name" : "3",
				                        "short_name" : "3",
				                        "types" : [ "street_number" ]
				                },
				                {
				                    "long_name" : "Rabbi Kook Street",
				                        "short_name" : "Rabbi Kook St",
				                        "types" : [ "route" ]
				                },
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "----------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rabbi Kook St 3, Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "location" : {
				                    "lat" : 31.9662242,
				                            "lng" : 34.8081116
				                },
				                "location_type" : "ROOFTOP",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 31.9675731802915,
				                                "lng" : 34.80946058029149
				                    },
				                    "southwest" : {
				                        "lat" : 31.9648752197085,
				                                "lng" : 34.8067626197085
				                    }
				                }
				            },
				                "place_id" : "ChIJc3lt5zm1AhURJUJEvPGF7Qo",
				                    "plus_code" : {
				                "compound_code" : "XR85+F6 Rishon LeTsiyon, Israel",
				                        "global_code" : "8G3PXR85+F6"
				            },
				                "types" : [ "establishment", "place_of_worship", "point_of_interest", "synagogue" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "3",
				                        "short_name" : "3",
				                        "types" : [ "street_number" ]
				                },
				                {
				                    "long_name" : "Rabbi Kook Street",
				                        "short_name" : "Rabbi Kook St",
				                        "types" : [ "route" ]
				                },
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rabbi Kook St 3, Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "location" : {
				                    "lat" : 31.9662242,
				                            "lng" : 34.8081116
				                },
				                "location_type" : "ROOFTOP",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 31.9675731802915,
				                                "lng" : 34.80946058029149
				                    },
				                    "southwest" : {
				                        "lat" : 31.9648752197085,
				                                "lng" : 34.8067626197085
				                    }
				                }
				            },
				                "place_id" : "ChIJ-Z0HkUi0AhUR-U4X5ghp5yY",
				                    "plus_code" : {
				                "compound_code" : "XR85+F6 Rishon LeTsiyon, Israel",
				                        "global_code" : "8G3PXR85+F6"
				            },
				                "types" : [ "street_address" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "3",
				                        "short_name" : "3",
				                        "types" : [ "street_number" ]
				                },
				                {
				                    "long_name" : "Rabbi Kook Street",
				                        "short_name" : "Rabbi Kook St",
				                        "types" : [ "route" ]
				                },
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rabbi Kook St 3, Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "location" : {
				                    "lat" : 31.9661104,
				                            "lng" : 34.8081301
				                },
				                "location_type" : "RANGE_INTERPOLATED",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 31.9674593802915,
				                                "lng" : 34.8094790802915
				                    },
				                    "southwest" : {
				                        "lat" : 31.9647614197085,
				                                "lng" : 34.8067811197085
				                    }
				                }
				            },
				                "place_id" : "EihSYWJiaSBLb29rIFN0IDMsIFJpc2hvbiBMZVRzaXlvbiwgSXNyYWVsIhoSGAoUChIJi2RLkki0AhURqFWHIC-p4nwQAw",
				                    "types" : [ "street_address" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "XR85+G6",
				                        "short_name" : "XR85+G6",
				                        "types" : [ "plus_code" ]
				                },
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "XR85+G6 Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 31.966375,
				                                "lng" : 34.808125
				                    },
				                    "southwest" : {
				                        "lat" : 31.96625,
				                                "lng" : 34.808
				                    }
				                },
				                "location" : {
				                    "lat" : 31.9663556,
				                            "lng" : 34.8080861
				                },
				                "location_type" : "GEOMETRIC_CENTER",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 31.96766148029149,
				                                "lng" : 34.80941148029149
				                    },
				                    "southwest" : {
				                        "lat" : 31.9649635197085,
				                                "lng" : 34.8067135197085
				                    }
				                }
				            },
				                "place_id" : "GhIJc06iFGP3P0ARFO2FXW9nQUA",
				                    "plus_code" : {
				                "compound_code" : "XR85+G6 Rishon LeTsiyon, Israel",
				                        "global_code" : "8G3PXR85+G6"
				            },
				                "types" : [ "plus_code" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "7-3",
				                        "short_name" : "7-3",
				                        "types" : [ "street_number" ]
				                },
				                {
				                    "long_name" : "Rabbi Kook Street",
				                        "short_name" : "Rabbi Kook St",
				                        "types" : [ "route" ]
				                },
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "-------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rabbi Kook St 7-3, Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 31.966181,
				                                "lng" : 34.808735
				                    },
				                    "southwest" : {
				                        "lat" : 31.966093,
				                                "lng" : 34.807981
				                    }
				                },
				                "location" : {
				                    "lat" : 31.966137,
				                            "lng" : 34.808358
				                },
				                "location_type" : "GEOMETRIC_CENTER",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 31.9674859802915,
				                                "lng" : 34.8097069802915
				                    },
				                    "southwest" : {
				                        "lat" : 31.9647880197085,
				                                "lng" : 34.8070090197085
				                    }
				                }
				            },
				                "place_id" : "ChIJi2RLkki0AhURqFWHIC-p4nw",
				                    "types" : [ "route" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "Rishon LeTsiyon",
				                        "short_name" : "-------",
				                        "types" : [ "locality", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 32.0107491,
				                                "lng" : 34.8441275
				                    },
				                    "southwest" : {
				                        "lat" : 31.9395029,
				                                "lng" : 34.7126462
				                    }
				                },
				                "location" : {
				                    "lat" : 31.9730015,
				                            "lng" : 34.7925013
				                },
				                "location_type" : "APPROXIMATE",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 32.0107491,
				                                "lng" : 34.8441275
				                    },
				                    "southwest" : {
				                        "lat" : 31.9395029,
				                                "lng" : 34.7126462
				                    }
				                }
				            },
				                "place_id" : "ChIJ_2arJzi0AhURuHoaV0rFvBc",
				                    "types" : [ "locality", "political" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "Ezor Rishon LeTsiyon",
				                        "short_name" : "Ezor Rishon LeTsiyon",
				                        "types" : [ "administrative_area_level_3", "political" ]
				                },
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Ezor Rishon LeTsiyon, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 32.010725,
				                                "lng" : 34.8441031
				                    },
				                    "southwest" : {
				                        "lat" : 31.86742599999999,
				                                "lng" : 34.682874
				                    }
				                },
				                "location" : {
				                    "lat" : 31.940514,
				                            "lng" : 34.7519807
				                },
				                "location_type" : "APPROXIMATE",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 32.010725,
				                                "lng" : 34.8441031
				                    },
				                    "southwest" : {
				                        "lat" : 31.86742599999999,
				                                "lng" : 34.682874
				                    }
				                }
				            },
				                "place_id" : "ChIJDxXuX2OxAhUR-CKLMJj8Az4",
				                    "types" : [ "administrative_area_level_3", "political" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "Rehovot",
				                        "short_name" : "Rehovot",
				                        "types" : [ "administrative_area_level_2", "political" ]
				                },
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Rehovot, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 32.0107249,
				                                "lng" : 34.8717699
				                    },
				                    "southwest" : {
				                        "lat" : 31.7571918,
				                                "lng" : 34.66654279999999
				                    }
				                },
				                "location" : {
				                    "lat" : 31.9005588,
				                            "lng" : 34.7519807
				                },
				                "location_type" : "APPROXIMATE",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 32.0107249,
				                                "lng" : 34.8717699
				                    },
				                    "southwest" : {
				                        "lat" : 31.7571918,
				                                "lng" : 34.66654279999999
				                    }
				                }
				            },
				                "place_id" : "ChIJl5dg3a2wAhURqr8kdIo9NtE",
				                    "types" : [ "administrative_area_level_2", "political" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "Center District",
				                        "short_name" : "Center District",
				                        "types" : [ "administrative_area_level_1", "political" ]
				                },
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Center District, Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 32.4126018,
				                                "lng" : 35.051422
				                    },
				                    "southwest" : {
				                        "lat" : 31.7571918,
				                                "lng" : 34.66654279999999
				                    }
				                },
				                "location" : {
				                    "lat" : 31.9521108,
				                            "lng" : 34.906551
				                },
				                "location_type" : "APPROXIMATE",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 32.4126018,
				                                "lng" : 35.051422
				                    },
				                    "southwest" : {
				                        "lat" : 31.7571918,
				                                "lng" : 34.66654279999999
				                    }
				                }
				            },
				                "place_id" : "ChIJ1dI8PjMVHRURwtHEUjGbcmU",
				                    "types" : [ "administrative_area_level_1", "political" ]
				            },
				            {
				                "address_components" : [
				                {
				                    "long_name" : "Israel",
				                        "short_name" : "IL",
				                        "types" : [ "country", "political" ]
				                }
				         ],
				                "formatted_address" : "Israel",
				                    "geometry" : {
				                "bounds" : {
				                    "northeast" : {
				                        "lat" : 33.33280500000001,
				                                "lng" : 35.896244
				                    },
				                    "southwest" : {
				                        "lat" : 29.47969999999999,
				                                "lng" : 34.2673871
				                    }
				                },
				                "location" : {
				                    "lat" : 31.046051,
				                            "lng" : 34.851612
				                },
				                "location_type" : "APPROXIMATE",
				                        "viewport" : {
				                    "northeast" : {
				                        "lat" : 33.33280500000001,
				                                "lng" : 35.896244
				                    },
				                    "southwest" : {
				                        "lat" : 29.47969999999999,
				                                "lng" : 34.2673871
				                    }
				                }
				            },
				                "place_id" : "ChIJi8mnMiRJABURuiw1EyBCa2o",
				                    "types" : [ "country", "political" ]
				            }
				   ],
				            "status" : "OK"
				}""";
	}
}
