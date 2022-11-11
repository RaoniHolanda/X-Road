/*
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.niis.xroad.cs.test.glue;

import com.nortal.test.asserts.Validation;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.niis.xroad.centralserver.openapi.model.CertificateAuthorityDto;
import org.niis.xroad.centralserver.openapi.model.OcspResponderDto;
import org.niis.xroad.cs.test.api.FeignCertificationServicesApi;
import org.niis.xroad.cs.test.api.FeignIntermediateCasApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

import static org.niis.xroad.cs.test.constants.Constants.KEY_OCSP_RESPONDER_ID;
import static org.niis.xroad.cs.test.utils.CertificateUtils.generateAuthCert;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


public class IntermediateCasApiStepDefs extends BaseStepDefs {

    private static final String KEY_INTERMEDIATE_CA_ID = "intermediateCaId";

    @Autowired
    private FeignCertificationServicesApi certificationServicesApi;
    @Autowired
    private FeignIntermediateCasApi intermediateCasApi;

    @When("intermediate CA added to certification service")
    public void addIntermediateCa() throws Exception {
        final Integer certificationServiceId = scenarioContext.getStepData("certificationServiceId");
        final MultipartFile certificate = new MockMultipartFile("certificate", generateAuthCert());
        final ResponseEntity<CertificateAuthorityDto> response = certificationServicesApi
                .addCertificationServiceIntermediateCa(certificationServiceId, certificate);

        final Validation.Builder validationBuilder = new Validation.Builder()
                .context(response)
                .title("Validate response")
                .assertion(equalsAssertion(CREATED, response.getStatusCode(), "Verify status code"));
        validationService.validate(validationBuilder.build());

        scenarioContext.putStepData(KEY_INTERMEDIATE_CA_ID, response.getBody().getId());
    }

    @When("OCSP responder is added to intermediate CA")
    public void ocspResponderIsAddedToIntermediateCA() throws Exception {
        final Integer intermediateCaId = scenarioContext.getStepData(KEY_INTERMEDIATE_CA_ID);
        final MultipartFile certificate = new MockMultipartFile("certificate", generateAuthCert());
        final String url = "https://" + UUID.randomUUID();

        final ResponseEntity<OcspResponderDto> response = intermediateCasApi
                .addIntermediateCaOcspResponder(intermediateCaId, url, certificate);

        final Validation.Builder validationBuilder = new Validation.Builder()
                .context(response)
                .title("Validate response")
                .assertion(equalsAssertion(CREATED, response.getStatusCode(), "Verify status code"));
        validationService.validate(validationBuilder.build());

        scenarioContext.putStepData(KEY_OCSP_RESPONDER_ID, response.getBody().getId());
    }

    @Then("intermediate CA has {int} OCSP responders")
    public void intermediateCAHasOCSPResponders(int count) {
        final Integer intermediateCaId = scenarioContext.getStepData(KEY_INTERMEDIATE_CA_ID);

        final ResponseEntity<Set<OcspResponderDto>> response = intermediateCasApi.getIntermediateCaOcspResponders(intermediateCaId);

        final Validation.Builder validationBuilder = new Validation.Builder()
                .context(response)
                .title("Validate response")
                .assertion(equalsAssertion(OK, response.getStatusCode(), "Verify status code"))
                .assertion(equalsAssertion(count, response.getBody().size(), "Response contains " + count + " items"));
        validationService.validate(validationBuilder.build());
    }

    @Then("intermediate CA has the updated OCSP responder")
    public void intermediateCAHasUpdatedOCSPResponder() {
        final Integer intermediateCaId = scenarioContext.getStepData(KEY_INTERMEDIATE_CA_ID);

        final ResponseEntity<Set<OcspResponderDto>> responseEntity = intermediateCasApi
                .getIntermediateCaOcspResponders(intermediateCaId);
        final OcspResponderDto response = responseEntity.getBody().iterator().next();

        final String newOcspResponderUrl = scenarioContext.getStepData("newOcspResponderUrl");

        final Validation.Builder validationBuilder = new Validation.Builder()
                .context(response)
                .title("Validate response")
                .assertion(equalsAssertion(OK, responseEntity.getStatusCode(), "Verify status code"))
                .assertion(equalsAssertion(Boolean.TRUE, response.getHasCertificate(), "Verify OCSP responder has certificate"))
                .assertion(equalsAssertion(newOcspResponderUrl, response.getUrl(), "OCSP responder url matches"));
        validationService.validate(validationBuilder.build());
    }

    @When("OCSP responder is deleted from intermediate CA")
    public void ocspResponderIsDeletedFromIntermediateCA() {
        final Integer intermediateCaId = scenarioContext.getStepData(KEY_INTERMEDIATE_CA_ID);
        final Integer ocspResponderId = scenarioContext.getStepData(KEY_OCSP_RESPONDER_ID);

        final ResponseEntity<Void> response = intermediateCasApi
                .deleteIntermediateCaOcspResponder(intermediateCaId, ocspResponderId);

        final Validation.Builder validationBuilder = new Validation.Builder()
                .context(response)
                .title("Validate response")
                .assertion(equalsAssertion(NO_CONTENT, response.getStatusCode(), "Verify status code"));
        validationService.validate(validationBuilder.build());
    }

}
