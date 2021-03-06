/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uniluebeck.itm.ncoap.message;

import de.uniluebeck.itm.ncoap.message.options.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.*;

/**
 * Instances of {@link de.uniluebeck.itm.ncoap.message.CoapRequest} are created by
 * {@link de.uniluebeck.itm.ncoap.application.client.CoapClientApplication}s to retrieve data from
 * remote CoAP Webservices.
 *
 * @author Oliver Kleine
 */
public class CoapRequest extends CoapMessage {

    private static Logger log = LoggerFactory.getLogger(CoapRequest.class.getName());

    private static final String NO_REQUEST_TYPE = "Message type %d is not a suitable type for requests (only CON and NON)!";
    private static final String NO_REQUEST_CODE = "Message code %d is not a request code!";
    private static final String URI_SCHEME = "URI scheme must be set to \"coap\" (but given URI is: %s)!";
    private static final String URI_FRAGMENT = "URI must not have a fragment (but given URI is: %s)!";

    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set. Using this constructor has the same effect as
     * using {@link #CoapRequest(MessageType.Name, MessageCode.Name, java.net.URI, boolean)} with <code>useProxy</code>
     * set to <code>false</code>.
     *
     * @param messageType  A {@link MessageType.Name}
     * @param messageCode A {@link MessageCode}
     * @param targetUri the recipients URI
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given arguments causes an error
     */
    public CoapRequest(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri)
            throws IllegalArgumentException {

        this(messageType.getNumber(), messageCode.getNumber(), targetUri, false);
    }


    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     *
     * @param messageType  A {@link MessageType.Name}
     * @param messageCode A {@link MessageCode}
     * @param targetUri the recipients URI
     * @param useProxy indicates if this {@link CoapRequest} is supposed to be sent to its final destination via
     *                 a forward-proxy (if set to <code>true</code> the given target URI is set as
     *                 {@link OptionValue.Name#PROXY_URI}, if set to <code>false</code> the given target URI is
     *                 set as combination of {@link OptionValue.Name#URI_HOST}, {@link OptionValue.Name#URI_PORT},
     *                 {@link OptionValue.Name#URI_PATH}, and {@link OptionValue.Name#URI_QUERY}.
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given arguments causes an error
     */
    public CoapRequest(MessageType.Name messageType, MessageCode.Name messageCode, URI targetUri, boolean useProxy)
            throws IllegalArgumentException {

        this(messageType.getNumber(), messageCode.getNumber(), targetUri, useProxy);
    }


    /**
     * Creates a new {@link CoapRequest} instance and uses the given parameters to create an appropriate header
     * and initial option list with target URI-related options set.
     *
     * @param messageType  A {@link MessageType}
     * @param messageCode A {@link MessageCode}
     * @param targetUri the recipients URI
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given arguments causes an error
     */
    public CoapRequest(int messageType, int messageCode, URI targetUri) throws IllegalArgumentException {
        this(messageType, messageCode, targetUri, false);
    }

    /**
     * Creates a {@link CoapRequest} instance with initial CoAP header and options according to the given parameters.
     *
     * @param messageType the number representing the message type for the {@link CoapRequest}.
     * @param messageCode the number representing the message code for the {@link CoapRequest}.
     * @param targetUri the {@link URI} representing the webservice this {@link CoapRequest} is to be sent to.
     * @param useProxy indicates if this {@link CoapRequest} is supposed to be sent to its final destination via
     *                 a forward-proxy (if set to <code>true</code> the given target URI is set as
     *                 {@link OptionValue.Name#PROXY_URI}, if set to <code>false</code> the given target URI is
     *                 set as combination of {@link OptionValue.Name#URI_HOST}, {@link OptionValue.Name#URI_PORT},
     *                 {@link OptionValue.Name#URI_PATH}, and {@link OptionValue.Name#URI_QUERY}.
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given arguments causes an error
     */
    public CoapRequest(int messageType, int messageCode, URI targetUri, boolean useProxy)
            throws IllegalArgumentException {

        this(messageType, messageCode);

        if(useProxy)
            setProxyURIOption(targetUri);
        else
            setTargetUriOptions(targetUri);

        log.debug("New request created: {}.", this);
    }

    /**
     * Creates a new instance of {@link CoapRequest}. <b>Note:</b> This constructor is only intended for internal use.
     * Please use one of the other constructors to avoid unexpected behaviour.
     *
     * @param messageType the number representing the {@link MessageType} for this {@link CoapRequest}
     * @param messageCode the number representing the {@link MessageCode} for this {@link CoapRequest}
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given arguments causes an error
     */
    public CoapRequest(int messageType, int messageCode) throws IllegalArgumentException {
        super(messageType, messageCode);

        if(messageType < MessageType.Name.CON.getNumber() || messageType > MessageType.Name.NON.getNumber())
            throw new IllegalArgumentException(String.format(NO_REQUEST_TYPE, messageType));

        if(!MessageCode.isRequest(messageCode))
            throw new IllegalArgumentException(String.format(NO_REQUEST_CODE, messageCode));
    }

    /**
     * Sets the proxy URI option of this {@link CoapRequest} with the given {@link URI}
     *
     * @param targetUri the final destination {@link URI} to send this {@link CoapRequest} to
     *
     * @throws IllegalArgumentException if the UTF-8 encoding of the given {@link URI} exceeds the maximum length
     * for {@link OptionValue.Name#PROXY_URI}.
     */
    private void setProxyURIOption(URI targetUri) throws IllegalArgumentException {
        this.addStringOption(OptionValue.Name.PROXY_URI, targetUri.toString());
    }


    private void setTargetUriOptions(URI targetUri) throws IllegalArgumentException {
        targetUri = targetUri.normalize();

        //URI must be absolute and thus contain a scheme part (must be one of "coap" or "coaps")
        String scheme = targetUri.getScheme();
        if(scheme == null)
            throw new IllegalArgumentException(String.format(URI_SCHEME, targetUri.toString()));

        scheme = scheme.toLowerCase(Locale.ENGLISH);
        if(!(scheme.equals("coap")))
            throw new IllegalArgumentException(String.format(URI_SCHEME, targetUri.toString()));

        //Target URI must not have fragment part
        if(targetUri.getFragment() != null)
            throw new IllegalArgumentException(String.format(URI_FRAGMENT, targetUri.toString()));

        //Create target URI options
        if(!(OptionValue.isDefaultValue(OptionValue.Name.URI_HOST, targetUri.getHost().getBytes(CoapMessage.CHARSET))))
            addUriHostOption(targetUri.getHost());

        if(targetUri.getPort() != -1 && targetUri.getPort() != OptionValue.URI_PORT_DEFAULT)
            addUriPortOption(targetUri.getPort());

        addUriPathOptions(targetUri.getPath());
        addUriQueryOptions(targetUri.getQuery());

    }


    private void addUriQueryOptions(String uriQuery) throws IllegalArgumentException {
        if(uriQuery != null){
            for(String queryComponent : uriQuery.split("&")){
                this.addStringOption(OptionValue.Name.URI_QUERY, queryComponent);
                log.debug("Added URI query option for {}", queryComponent);
            }
        }
    }

    
    private void addUriPathOptions(String uriPath) throws IllegalArgumentException {
        if(uriPath != null){
            //Path must not start with "/" to be further processed
            if(uriPath.startsWith("/"))
                uriPath = uriPath.substring(1);

            if("".equals(uriPath))
                return;

            for(String pathComponent : uriPath.split("/")){
                this.addStringOption(OptionValue.Name.URI_PATH, pathComponent);
                log.debug("Added URI path option for {}", pathComponent);
            }
        }
    }


    private void addUriPortOption(int uriPort) throws IllegalArgumentException {
        if(uriPort > 0 && uriPort != OptionValue.URI_PORT_DEFAULT)
            this.addUintOption(OptionValue.Name.URI_PORT, uriPort);
    }


    private void addUriHostOption(String uriHost) throws IllegalArgumentException {
        addStringOption(OptionValue.Name.URI_HOST, uriHost);
    }


    /**
     * Sets the If-Match options according to the given {@link Collection} containing ETAGs. If there were any
     * If-Match options present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} prior to the invocation of
     * this method, these options are removed.
     *
     * @param etags the ETAGs to be set as values for the If-Match options
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given <code>byte[]</code> to be set as values
     * for If-Match options is invalid.
     */
    public void setIfMatch(byte[]... etags) throws IllegalArgumentException {

        this.removeOptions(OptionValue.Name.IF_MATCH);

        try{
            for(byte[] etag : etags)
                this.addOpaqueOption(OptionValue.Name.IF_MATCH, etag);
        }
        catch (IllegalArgumentException e) {
            this.removeOptions(OptionValue.Name.IF_MATCH);
            throw e;
        }
    }

    /**
     * Returns a {@link java.util.Set<>} containing the values of the If-Match options. If no such option is present in
     * this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} the returned set is empty.
     *
     * @return a {@link java.util.Set<>} containing the values of the If-Match options. If no such option is present in
     * this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} the returned set is empty.
     */
    public Set<byte[]> getIfMatch(){

        Set<OptionValue> ifMatchOptionValues = options.get(OptionValue.Name.IF_MATCH);
        Set<byte[]> result = new HashSet<>(ifMatchOptionValues.size());

        for (OptionValue ifMatchOptionValue : ifMatchOptionValues)
                result.add(((OpaqueOptionValue) ifMatchOptionValue).getDecodedValue());

        return result;
    }

    /**
     * Returns the value of the URI host option or a literal representation of the recipients IP address if the URI
     * host option is not present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     *
     * @return the value of the URI host option or <code>null</code> if the URI
     * host option is not present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     */
    public String getUriHost(){

        if(options.containsKey(OptionValue.Name.URI_HOST))
            return ((StringOptionValue) options.get(OptionValue.Name.URI_HOST).iterator().next()).getDecodedValue();

        return null;
    }



    /**
     * Sets the ETAG options of this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}. If there are any ETAG options
     * present in this request prior to the invocation of this method, those options are removed.
     *
     * @param etags the values for the ETAG options to be set
     *
     * @throws java.lang.IllegalArgumentException if at least one of the given ETAGs is not suitable to be the value of
     * an ETAG option.
     */
    public void setEtags(byte[]... etags) throws IllegalArgumentException {

        this.removeOptions(OptionValue.Name.ETAG);

        try{
            for(byte[] etag : etags)
                this.addOpaqueOption(OptionValue.Name.ETAG, etag);
        }
        catch(IllegalArgumentException e){
            this.removeOptions(OptionValue.Name.ETAG);
            throw e;
        }
    }

    /**
     * Returns a {@link Set} containing the values of the ETAG options that are present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}. If there is no such option, then the returned set is empty.
     *
     * @return  a {@link Set} containing the values of the ETAG options that are present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}. If there is no such option, then the returned set is empty.
     */
    public Set<byte[]> getEtags(){
        Set<byte[]> result = new HashSet<>();

        for (OptionValue optionValue : options.get(OptionValue.Name.ETAG))
            result.add(((OpaqueOptionValue) optionValue).getDecodedValue());

        return result;
    }

    /**
     * Sets the If-Non-Match option in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} and returns
     * <code>true</code> if the option is set after method returns (may already have been set beforehand in a prior
     * method invocation) or <code>false</code if the option is not set, e.g. because that option has no meaning with
     * the message code of this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}
     *
     * @return <code>true</code> if the option is set after method returned or <code>false</code> otherwise.
     */
    public boolean setIfNonMatch() {
        if(options.containsKey(OptionValue.Name.IF_NONE_MATCH))
            return true;

        try{
            this.addEmptyOption(OptionValue.Name.IF_NONE_MATCH);
            return true;
        }
        catch(IllegalArgumentException e){
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the If-Non-Match option is present or <code>false</code> if there is
     * no such option present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     *
      * @return <code>true</code> if the If-Non-Match option is present or <code>false</code> if there is
     * no such option present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     */
    public boolean isIfNonMatchSet(){
        return options.containsKey(OptionValue.Name.IF_NONE_MATCH);
    }


    /**
     * Returns the value of the URI port option or {@link OptionValue#URI_PORT_DEFAULT} if the URI port option is not
     * present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     *
     * @return the value of the URI port option or {@link OptionValue#URI_PORT_DEFAULT} if the URI port option is not
     * present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     */
    public long getUriPort(){
        if(options.containsKey(OptionValue.Name.URI_PORT))
            return ((UintOptionValue) options.get(OptionValue.Name.URI_PORT).iterator().next()).getDecodedValue();

        return OptionValue.URI_PORT_DEFAULT;
    }


    /**
     * Returns the full path of the request URI reconstructed from the URI path options present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}. If no such option is set, the returned value is "/".
     *
     * @return the full path of the request URI reconstructed from the URI path options present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest}.
     */
    public String getUriPath(){
        String result = "/";

        Iterator<OptionValue> iterator = options.get(OptionValue.Name.URI_PATH).iterator();
        if(iterator.hasNext())
            result += ((StringOptionValue) iterator.next()).getDecodedValue();

        while(iterator.hasNext())
            result += ("/" + ((StringOptionValue) iterator.next()).getDecodedValue());

        return result;
    }

    /**
     * Returns the full query of the request URI reconstructed from the URI query options present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} or the empty string ("") if no such option is present.
     *
     * @return the full query of the request URI reconstructed from the URI query options present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} or the empty string ("") if no such option is present.
     */
    public String getUriQuery(){
        String result = "";

        if(options.containsKey(OptionValue.Name.URI_QUERY)){

            Iterator<OptionValue> iterator = options.get(OptionValue.Name.URI_QUERY).iterator();
            result += (((StringOptionValue) iterator.next()).getDecodedValue());

            while(iterator.hasNext())
                result += ("&" + ((StringOptionValue) iterator.next()).getDecodedValue());

        }

        return result;
    }

    /**
     * Returns the value of the qiven query parameter from the contained values of {@link OptionValue.Name#URI_QUERY}
     * or <code>null</code> if no such {@link OptionValue.Name#URI_QUERY} is present.
     *
     * Assume, this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} contains a {@link OptionValue.Name#URI_QUERY}
     * "param1=example", then this method invocation with parameter set to "param1" (or "param1=") returns "example".
     *
     * @param parameter the parameter string to look up the value for.
     *
     * @return the value of the qiven query parameter from the contained values of {@link OptionValue.Name#URI_QUERY}
     * or <code>null</code> if no such {@link OptionValue.Name#URI_QUERY} is present.
     */
    public String getUriQueryParameterValue(String parameter){
        if(!parameter.endsWith("="))
            parameter += "=";

        for(OptionValue optionValue : options.get(OptionValue.Name.URI_QUERY)){
            String value = ((StringOptionValue) optionValue).getDecodedValue();

            if(value.startsWith(parameter))
                return value.substring(parameter.length());
        }

        return null;
    }


    /**
     * Sets the content formats the client is willing to accept. See
     * {@link de.uniluebeck.itm.ncoap.message.options.ContentFormat} for a predefined set of such
     * numbers. <b>Note:</b> If this method throws an {@link java.lang.IllegalArgumentException}, all
     * {@link OptionValue.Name#ACCEPT} are removed from this {@link CoapRequest}.
     *
     * @param contentFormatNumbers a {@link java.util.Collection} containing the content formats the client is willing
     *                             to accept.
     *
     * @throws java.lang.IllegalArgumentException if one of the given numbers is not capable to represent a content
     * format
     */
    public void setAccept(long... contentFormatNumbers) throws IllegalArgumentException {
        options.removeAll(OptionValue.Name.ACCEPT);
        try{
            for(long contentFormatNumber : contentFormatNumbers)
                this.addUintOption(OptionValue.Name.ACCEPT, contentFormatNumber);
        }
        catch (IllegalArgumentException e) {
            options.removeAll(OptionValue.Name.ACCEPT);
            throw e;
        }
    }


    /**
     * Returns a {@link Set<Long>} containing the numbers representing the accepted content formats. See
     * {@link de.uniluebeck.itm.ncoap.message.options.ContentFormat} for a predefined set of such numbers. If no such
     * option is present in this {@link de.uniluebeck.itm.ncoap.message.CoapRequest}, then the returned set is empty.
     *
     * @return a {@link Set<Long>} containing the numbers representing the accepted content formats.
     */
    public Set<Long> getAcceptedContentFormats(){
        Set<Long> result = new HashSet<>();

        for(OptionValue optionValue : options.get(OptionValue.Name.ACCEPT))
            result.add(((UintOptionValue) optionValue).getDecodedValue());

        return result;
    }


    /**
     * Returns the value of the Proxy URI option if such an option is present in this {@link CoapRequest}. If no such
     * option is present but a Proxy Scheme option, then the returned {@link java.net.URI} is reconstructed from the
     * Proxy Scheme option and the URI host, URI port, URI path and URI query options.
     *
     * If both options, Proxy URI and Proxy Scheme are not present in this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} this method returns <code>null</code>.
     *
     * @return the URI of the requested resource if this {@link de.uniluebeck.itm.ncoap.message.CoapRequest} was
     * (or is supposed to be) sent via a proxy or <code>null</code> if the request was (or is supposed to be) sent
     * directly.
     *
     * @throws URISyntaxException if the value of the proxy URI option or the reconstruction from Proxy Scheme,
     * URI host, URI port, URI path, and URI query options is invalid.
     */
    public URI getProxyURI() throws URISyntaxException {
        if(options.containsKey(OptionValue.Name.PROXY_URI)){
            OptionValue proxyUriOptionValue = options.get(OptionValue.Name.PROXY_URI).iterator().next();
            return new URI(((StringOptionValue) proxyUriOptionValue).getDecodedValue());
        }

        if(options.get(OptionValue.Name.PROXY_SCHEME).size() == 1){
            OptionValue proxySchemeOptionValue = options.get(OptionValue.Name.PROXY_SCHEME).iterator().next();
            String scheme = ((StringOptionValue) proxySchemeOptionValue).getDecodedValue();
            String uriHost = getUriHost();
            OptionValue uriPortOptionValue = options.get(OptionValue.Name.URI_PORT).iterator().next();
            int uriPort = ((UintOptionValue) uriPortOptionValue).getDecodedValue().intValue();
            String uriPath = getUriPath();
            String uriQuery = getUriQuery();

            return new URI(scheme, null, uriHost, uriPort == OptionValue.URI_PORT_DEFAULT ? -1 : uriPort, uriPath,
                    uriQuery, null);
        }

        return null;
    }

    /**
     * Returns <code>true</code> if the observing option is set on this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} or <code>false</code> otherwise.
     *
     * @return <code>true</code> if the observing option is set on this
     * {@link de.uniluebeck.itm.ncoap.message.CoapRequest} or <code>false</code> otherwise.
     */
    public boolean isObservationRequest(){
        return(!options.get(OptionValue.Name.OBSERVE).isEmpty());
    }
}
