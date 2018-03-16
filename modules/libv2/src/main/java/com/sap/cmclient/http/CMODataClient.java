package com.sap.cmclient.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.primitives.Ints.asList;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.sap.cmclient.dto.Transport;
import com.sap.cmclient.dto.TransportMarshaller;

public class CMODataClient {

    private static class Entities {
        private Entities() {}
        final static String TRANSPORTS = "Transports";
    }

    private final URI endpoint;
    private final HttpClientFactory clientFactory;
    private String token = null;
    private Edm dataModel = null;

    public CMODataClient(String endpoint, String user, String password) throws URISyntaxException {

        this.endpoint = new URI(endpoint);


        // the same instance needs to be used as long as we are in the same session. Hence multiple
        // clients must share the same cookie store. Reason: we are logged on with the first request
        // and get a cookie. That cookie - expressing the user has already been logged in - needs to be
        // present in all subsequent requests.
        CookieStore sessionCookieStore = new BasicCookieStore();

        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        basicCredentialsProvider.setCredentials(
                new AuthScope(this.endpoint.getHost(), this.endpoint.getPort()),
                new UsernamePasswordCredentials(user, password));

        this.clientFactory = new HttpClientFactory(sessionCookieStore, basicCredentialsProvider);
    }

  // args[0]
  // "http://wdflbmd16301.wdf.sap.corp:8000/sap/opu/odata/SAP/SCTS_CLOUD_API_ODATA_SRV/Transports(\'A5DK900014\')"
  // args[1] ODATA
  // args[2] <the password ...>

  // In order to run it with wire logging use:
  // -Dorg.slf4j.simpleLogger.defaultLogLevel=trace
  // -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
  // -Dorg.apache.commons.logging.simplelog.showdatetime=true
  // -Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG
  // -Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=DEBUG

  public final static void main(String[] args) throws Exception
  {
    CMODataClient odataClient = new CMODataClient(args[0], args[1], args[2]);
    
    GregorianCalendar cal = new GregorianCalendar();
    GregorianCalendar time = new GregorianCalendar(0, 0, 0, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    //Transport myTrans = new Transport("", args[1], "Testtransport via ODATA", "A5T", cal, time, "", "X", Transport.Status.D, Transport.Type.K);
    //Transport created = odataClient.createTransport(myTrans);
    //System.out.println(created);
    //created.setStatus(Transport.Status.R);
    
    
    //Transport read = odataClient.getTransport("A5DK900028");
    //System.out.println(read);
    //read.setDescription("Change by OData");
    //odataClient.updateTransport(read);
    //System.out.println(read);
    
     //odataClient.deleteTransport(read.getId());
    //transport.setDescription("Dies ist per OData geaendert");
    //odataClient.updateTransport(transport);
    
  }

  public Transport getTransport(String transportId) throws IOException, EntityProviderException, EdmException, UnexpectedHttpResponseException {

      final TransportRequestBuilder builder = new TransportRequestBuilder(endpoint);

      try (CloseableHttpClient client = clientFactory.createClient()) {
          HttpUriRequest get = builder.getTransport(transportId);
          try(CloseableHttpResponse response = client.execute(get)) {
              Header[] contentType = response.getHeaders(HttpHeaders.CONTENT_TYPE);
              checkStatusCode(response, SC_OK, SC_NOT_FOUND, 500); // 500 is currently returned in case the transport cannot be found.
              if (Arrays.asList(SC_OK).contains(response.getStatusLine().getStatusCode())) {
                  try(InputStream content = response.getEntity().getContent()) {
                  return TransportMarshaller.get(EntityProvider.readEntry(contentType[0].getValue(),
                                                 getEntityDataModel().getDefaultEntityContainer()
                                                     .getEntitySet(TransportRequestBuilder.getEntityKey()),
                                                 content,
                                                 EntityProviderReadProperties.init().build()));
                  }
              }
          }

          return null;
    }
  }

  public void updateTransport(Transport transport) throws IOException, URISyntaxException, ODataException, UnexpectedHttpResponseException
  {
    final TransportRequestBuilder builder = new TransportRequestBuilder(endpoint);
    Edm edm = getEntityDataModel();
    try (CloseableHttpClient client = clientFactory.createClient()) {
      
      HttpPut put = builder.updateTransport(transport.getId());
      put.setHeader("x-csrf-token", getCSRFToken());
      EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
      EdmEntitySet entitySet = entityContainer.getEntitySet(TransportRequestBuilder.getEntityKey());
      URI rootUri = new URI(endpoint.toASCIIString() + "/");
      EntityProviderWriteProperties properties = EntityProviderWriteProperties.serviceRoot(rootUri).build();
      ODataResponse response = null;
      try {
          response = EntityProvider.writeEntry(put.getHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue(), entitySet, TransportMarshaller.put(transport), properties);
          put.setEntity(EntityBuilder.create().setStream(response.getEntityAsStream()).build());
          try (CloseableHttpResponse httpResponse = client.execute(put)) {
              checkStatusCode(httpResponse, SC_OK, HttpStatus.SC_NO_CONTENT);
          }
      }  finally {
          if(response != null) response.close();
      }
    }
  }
  public Transport createTransport(Transport transport) throws IOException, URISyntaxException, ODataException, UnexpectedHttpResponseException
  {
    final TransportRequestBuilder builder = new TransportRequestBuilder(endpoint);
    Edm edm = getEntityDataModel();
    try (CloseableHttpClient client = clientFactory.createClient()) {
      HttpPost post = builder.createTransport();
      post.setHeader("x-csrf-token", getCSRFToken());
      EdmEntityContainer entityContainer = edm.getDefaultEntityContainer();
      EdmEntitySet entitySet = entityContainer.getEntitySet(TransportRequestBuilder.getEntityKey());
      URI rootUri = new URI(endpoint.toASCIIString() + "/");
      EntityProviderWriteProperties properties = EntityProviderWriteProperties.serviceRoot(rootUri).build();
      ODataResponse response = null;

      try {
        response = EntityProvider.writeEntry(post.getHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue(), entitySet, TransportMarshaller.put(transport), properties);
        post.setEntity(EntityBuilder.create().setStream(response.getEntityAsStream()).build());

        try(CloseableHttpResponse httpResponse = client.execute(post)) {
          Header[] contentType = httpResponse.getHeaders(HttpHeaders.CONTENT_TYPE);
          checkStatusCode(httpResponse, SC_CREATED);
          if (Arrays.asList(SC_CREATED).contains(httpResponse.getStatusLine().getStatusCode())) {
            return TransportMarshaller.get(EntityProvider.readEntry(contentType[0].getValue(),
                       edm.getDefaultEntityContainer()
                      .getEntitySet(TransportRequestBuilder.getEntityKey()),
                        httpResponse.getEntity().getContent(), EntityProviderReadProperties.init().build()));
          }
        }
      } finally {
          if(response != null) response.close();
      }

      return null;
    }
  }

  public void deleteTransport(String id) throws EntityProviderException, IOException, UnexpectedHttpResponseException
  {
    final TransportRequestBuilder builder = new TransportRequestBuilder(endpoint);
    try (CloseableHttpClient client = clientFactory.createClient()) {
      HttpDelete delete = builder.deleteTransport(id);
      delete.setHeader("x-csrf-token", getCSRFToken());
      try(CloseableHttpResponse response = client.execute(delete)) {
        checkStatusCode(response, HttpStatus.SC_NO_CONTENT);
      }
    }


  }

    public String upload(String transportId, File content) throws IOException, CMODataClientException, UnexpectedHttpResponseException {

        checkArgument(content.canRead(), format("Cannot read file '%s'.", content.getAbsolutePath()));

        try(InputStream c = new FileInputStream(content)) {
            return upload(transportId, c);
        }
    }

    public String upload(String transportId, InputStream content) throws IOException, CMODataClientException, UnexpectedHttpResponseException {

        checkArgument(! isNullOrEmpty(transportId), "TransportId is null or empty.");
        checkArgument(content != null, "No content provided for upload.");

        //
        // Below we work with a non-repeatable entity. This is fine since we can be sure the
        // request does not need to be repeated. Reason: the need for repeating a request inside
        // the client (transparent for the caller) is an authentification scenario (first request
        // without credentials, resulting in 401, repeated request with credentials). When uploading
        // the content we are already authenticated since we have to fetch the csrf token beforehand
        // in any case.
        //

        try (CloseableHttpClient client = clientFactory.createClient()) {
            HttpPost request = new HttpPost(endpoint + "/" + Entities.TRANSPORTS + "('" + transportId + "')/File");
            request.setEntity(new InputStreamEntity(content));
            request.addHeader("x-csrf-token", getCSRFToken());
            try (CloseableHttpResponse response = client.execute(request)) {
                checkStatusCode(response, HttpStatus.SC_CREATED);
                return response.getHeaders("location")[0].getValue();
            }
        }
    }

    private Edm getEntityDataModel() throws IOException, EntityProviderException, UnexpectedHttpResponseException {

        if(dataModel == null) {
            try (CloseableHttpClient edmClient = clientFactory.createClient()) {
                try (CloseableHttpResponse response = edmClient.execute(new HttpGet(endpoint.toASCIIString() + "/" + "$metadata"))) {
                    checkStatusCode(response, SC_OK);
                    dataModel = EntityProvider.readMetadata(response.getEntity().getContent(), false);
                }
            }
        }

        return dataModel;
    }

    private String getCSRFToken() throws ClientProtocolException, IOException {

        if(this.token == null) {
            HttpGet httpGet = new HttpGet(this.endpoint);
            httpGet.addHeader("X-CSRF-Token", "Fetch");
            httpGet.addHeader("Accept", "application/xml");
            try(CloseableHttpClient client = clientFactory.createClient()) {
                try(CloseableHttpResponse response = client.execute(httpGet)) {
                    Header[] csrfHeaders = response.getHeaders("x-csrf-token");
                    if(csrfHeaders.length != 1) throw new IllegalStateException("Multiple or no csrfHeaders received.");
                    this.token = csrfHeaders[0].getValue();
                }
            }
        }

        return this.token;
    }

    private static void checkStatusCode(HttpResponse response, int...  expected) throws UnexpectedHttpResponseException, UnsupportedOperationException, IOException {

        if( ! asList(expected).contains(response.getStatusLine().getStatusCode())) {

            try(InputStream content = response.getEntity().getContent()) {

                // TODO hard coded char set is a best guess. Should be retrieved from response.
                String reason = CharStreams.toString(new InputStreamReader(content, Charsets.UTF_8));
                throw new UnexpectedHttpResponseException(format("Unexpected response code '%d'. Reason: %s", response.getStatusLine().getStatusCode(), reason),
                                                response.getStatusLine());
            }
        }
    }
}