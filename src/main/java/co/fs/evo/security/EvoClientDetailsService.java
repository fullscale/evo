package co.fs.evo.security;

import static co.fs.evo.Constants.SYSTEM_INDEX;

import java.util.ArrayList;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.NoSuchClientException;

import co.fs.evo.services.SearchService;

public class EvoClientDetailsService implements ClientDetailsService {

	private static final XLogger logger = XLoggerFactory.getXLogger(EvoClientDetailsService.class);
	
    @Autowired private SearchService searchService;
	
    /**
     * Returns the user account details as a Java Map.
     */
    private Map<String, Object> getUser(String clientId) {
        GetResponse user = searchService.getDoc(SYSTEM_INDEX, "clients", clientId, null);
        return user.sourceAsMap();
    }
    
    @SuppressWarnings("unchecked")
	@Override
	public ClientDetails loadClientByClientId(String clientId)
			throws ClientRegistrationException {

        logger.entry(clientId);
        Map<String, Object> source = getUser(clientId);
        EvoClientDetails clientDetails = null;

        if (source != null) {
            clientDetails = new EvoClientDetails();
            clientDetails.setClientId((String) source.get("clientId"));
            clientDetails.setResourceIds((ArrayList<String>)source.get("resourceIds"));
            clientDetails.setIsSecretRequired((Boolean) source.get("secretRequired"));
            clientDetails.setClientSecret((String) source.get("clientSecret"));
            clientDetails.setIsScoped((Boolean) source.get("isScoped"));
            clientDetails.setScope((ArrayList<String>) source.get("credentialsNonExpired"));
            clientDetails.setAuthorizedGrantTypes((ArrayList<String>) source.get("enabled"));
            clientDetails.setAuthorities((ArrayList<String>) source.get("authorities"));
            clientDetails.setAccessTokenValiditySeconds((int) source.get("accessTokenValiditySeconds"));
            clientDetails.setRefreshTokenValiditySeconds((int) source.get("refreshTokenValiditySeconds"));
        } else {
            throw new NoSuchClientException("Unknown client: " + clientId);
        }

        logger.exit();
        return clientDetails;
	}

}
