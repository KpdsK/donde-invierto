package spark.template.velocity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
//import javax.activation.DataSource;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.password.PasswordEncoder;
import org.pac4j.core.credentials.password.SpringSecurityPasswordEncoder;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oauth.profile.facebook.FacebookPicture;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.sparkjava.CallbackRoute;
import org.pac4j.sparkjava.LogoutRoute;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.pac4j.sql.profile.service.DbProfileService;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import ar.edu.utn.frba.dds.dondeinvierto.jpa.Cuenta;
import ar.edu.utn.frba.dds.dondeinvierto.jpa.Indicador;
import ar.edu.utn.frba.dds.dondeinvierto.jpa.ManejadorPersistencia;

import static spark.Spark.before;

import spark.*;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import static spark.Spark.*;
import static spark.debug.DebugScreen.*;

/**
 * VelocityTemplateRoute example.
 */
public final class Home3 {
	private static java.util.List<CommonProfile> getProfiles(final Request request,
			final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		return manager.getAll(true);
	}   
	private static Map buildModel(Request request, Response response) {

		final Map model = new HashMap<String,Object>();

		Map<String, Object> map = new HashMap<String, Object>();
		for (String k: request.session().attributes()) {
			Object v = request.session().attribute(k);
			map.put(k,v);
		}

		model.put("session", map.entrySet());

		java.util.List<CommonProfile> userProfiles = getProfiles(request,response);

		model.put("profiles", userProfiles);

		try {
			if (userProfiles.size()>0) {
				CommonProfile firstProfile = userProfiles.get(0);
				model.put("firstProfile", firstProfile);	
				model.put("name",firstProfile.getDisplayName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;	
	}
	static Configuration configuration = new Configuration().configure();
	static ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();        
	static SessionFactory sf = configuration.buildSessionFactory(serviceRegistry);
	//	 sf = new Configuration().configure().buildSessionFactory(serviceRegistry);
	public static void main(String[] args) {

		// directorio donde se encuantran los recursos que usa la webApp (ej.:
		// html, img, css, bootstrap, templates, etc.)
		staticFiles.location("/publico"); // Static files
		FacebookClient facebookClient = new FacebookClient("1091856944283517", "4d43d2452ebe853ba63a1095c1a41e75");
		final TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA", "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");

		final Clients clients = new Clients("http://localhost:4567/callback", facebookClient, twitterClient);
		final Config config = new Config(clients);
		config.setHttpActionAdapter(new DemoHttpActionAdapter(new VelocityTemplateEngine()));
		
		enableDebugScreen();

        File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

		
		get("/ingreso", (request, response) -> {
			Map<String, Object> model = new HashMap<>();
			model.put("ingresoValido", true);
			return new ModelAndView(model, "publico/pages/login.vm"); // located in the resources directory
		}, new VelocityTemplateEngine());

		post("/ingreso", (request, response) -> {
			Map<String, Object> model = new HashMap<>();
			model.put("ingresoValido", true);
			return new ModelAndView(model, "publico/pages/login.vm"); // located in the resources directory
		}, new VelocityTemplateEngine());

		before("/loginFacebook", new SecurityFilter(config, "FacebookClient"));

		get("/loginFacebook",
				(request, response) -> { 
				return new ModelAndView(buildModel(request, response), "publico/pages/inicio.vm"); // located in the resources directory
				}, new VelocityTemplateEngine());

		before("/loginTwitter", new SecurityFilter(config, "TwitterClient"));

		get("/loginTwitter",
				(request, response) -> { 
				return new ModelAndView(buildModel(request, response), "publico/pages/inicio.vm"); // located in the resources directory
				}, new VelocityTemplateEngine());

		get("/crear-ind",
				(request, response) -> { Map<String, Object> model = new HashMap<>();
				model.put("guardadoExitoso", true);
				return new ModelAndView(model, "publico/pages/crear-ind.vm"); // located in the resources directory
				}, new VelocityTemplateEngine());
		get("/consultar-ind",
				(request, response) -> {
				return new ModelAndView(obtenerDatosParaTablaIndicadores(), "publico/pages/consultar-ind.vm"); // located in the resources directory
				}, new VelocityTemplateEngine());
		get("/loginDB", Home3::formulario, new VelocityTemplateEngine());
		post("/loginDB", Home3::formulario, new VelocityTemplateEngine());
		get("/logout", new LogoutRoute(config, "/ingreso"));
		get("/inicio", (request, response) -> {
			Map<String, Object> model = new HashMap<>();
			return new ModelAndView(buildModel(request, response), "publico/pages/inicio.vm"); // located in the resources directory
		}, new VelocityTemplateEngine());
		CallbackRoute callback = new CallbackRoute(config, null, true);

		get("/callback", callback);
		post("/callback", callback);
		
		post("/guardar-indicador", (request, response) -> {
			Map<String, Object> mapDatos =asMap(request.body(),"UTF-8");
		    Map<String, Object> model = new HashMap<>();
		    model.put("guardadoExitoso", crearIndicador((String)mapDatos.get("nombre"),(String)mapDatos.get("expresion")));
		    return new ModelAndView(model, "publico/pages/crear-ind.vm");
		}, new VelocityTemplateEngine());
		
		get("/consultar-cuentas",
				(request, response) -> { Map<String, Object> model = new HashMap<>();
				return new ModelAndView(new HashMap<String,Object>(), "publico/pages/consultar-cuentas.vm"); // located in the resources directory
				}, new VelocityTemplateEngine());
		post("/guardar-cuenta", (req, res) -> {

            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logInfo(req, tempFile);

			String mostrar = "vacio";
            File file = new File(tempFile.getFileName().toString());
            try {
    			Scanner inputStream = new Scanner(file);

				String linea1 = inputStream.next();//ignora la primer linea del archivo que tiene los titulos
    			while (inputStream.hasNext()){
    				String linea = inputStream.next();
    				String[] datos = linea.split(",");
    				
    			    Map<String, Object> model = new HashMap<>();
    			    model.put("guardadoExitoso", crearCuenta(datos[0],datos[1],datos[2],datos[3]));
    			    return new ModelAndView(model, "publico/pages/cargar-cuentas.vm");

    			}
    			inputStream.close();
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

		return "<h1>Archivo cargado<h1>"+ mostrar;
            
        });
		
		// Manejo de errores de pagina. Using string/html
		notFound("<html><body><h1>Error 404 no existe la pagina</h1></body></html>");
	}
	
	// methods used for logging
    private static void logInfo(Request req, Path tempFile) throws IOException, ServletException {
        System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile.toAbsolutePath() + "'");
    }

    private static String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

<<<<<<< HEAD
	private static boolean crearCuenta(String empresa, String cuenta, String valor, String periodo) {
		Map<String, Object> map= new HashMap<>();
		try {
			ManejadorPersistencia.persistir(new Cuenta().setEmpresa(empresa).setNombre(cuenta).setValor(valor).setPeriodo(periodo));
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
=======
	private static Map<String, Object> obtenerDatosParaTablaIndicadores() {
		EntityManager em = ManejadorPersistencia.INSTANCE.getEntityManager();
		List<Indicador> indicadores = em.createQuery("SELECT i FROM Indicador i", Indicador.class).getResultList();
		String datos="";
		for (Indicador i : indicadores) {
			datos = datos.concat("[\""+i.getNombre()+"\",\""+i.getExpresion()+"\"],");
		}
		Map<String, Object> map= new HashMap<>();
		if (!datos.isEmpty())
			datos="["+datos.substring(0, datos.length()-1)+"];";
		map.put("datosTabla", datos);
		return map;
	}
>>>>>>> branch 'master' of https://github.com/KpdsK/donde-invierto.git
	private static boolean crearIndicador(String nombre, String expresion) {
		Map<String, Object> map= new HashMap<>();
		try {
			ManejadorPersistencia.persistir(new Indicador().setNombre(nombre).setExpresion(expresion));
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	private static ModelAndView formulario(final Request request, final Response response) throws HttpAction, UnsupportedEncodingException {
		Map<String, Object> mapDatos =asMap(request.body(),"UTF-8");
		PasswordEncoder PASSWORD_ENCODER = new SpringSecurityPasswordEncoder(new StandardPasswordEncoder());
		MysqlDataSource mSqlDS = new MysqlDataSource();
		mSqlDS.setURL("jdbc:mysql://localhost:3306/donde_invierto");
		mSqlDS.setUser("pds");
		mSqlDS.setPassword("clave");
		final DbProfileService dbProfileService = new DbProfileService(mSqlDS,"nombre");
		dbProfileService.setPasswordEncoder(PASSWORD_ENCODER);
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials((String) mapDatos.get("username"), (String) mapDatos.get("password"), "");
		final Map map = new HashMap();
		try {
			dbProfileService.validate(credentials, null);
		} catch (CredentialsException e) {
			map.put("ingresoInvalido", "inputError");
			map.put("error", "has-error");
			map.put("ingresoValido", false);
			return new ModelAndView(map, "publico/pages/login.vm");
		}
		Session session = sf.openSession();
		map.put("name", credentials.getUserProfile().getId());
		return new ModelAndView(map, "publico/pages/inicio.vm");
	}
	
	public static Map<String, Object> asMap(String urlencoded, String encoding) throws UnsupportedEncodingException {

	    Map<String, Object> map = new LinkedHashMap<>();

	    for (String keyValue : urlencoded.trim().split("&")) {

	      String[] tokens = keyValue.trim().split("=");
	      String key = tokens[0];
	      String value = tokens.length == 1 ? null : URLDecoder.decode(tokens[1], encoding);

	      String[] keys = key.split("\\.");
	      Map<String, Object> pointer = map;

	      for (int i = 0; i < keys.length - 1; i++) {

	        String currentKey = keys[i];
	        Map<String, Object> nested = (Map<String, Object>) pointer.get(keys[i]);

	        if (nested == null) {
	          nested = new LinkedHashMap<>();
	        }

	        pointer.put(currentKey, nested);
	        pointer = nested;
	      }

	      pointer.put(keys[keys.length - 1], value);
	    }

	    return map;
	  }
}