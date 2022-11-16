/*
 * The MIT License
 *
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import {
  ApprovedCertificationService,
  ApprovedCertificationServiceListItem, CertificateAuthority, CertificateDetails,
  CertificationServiceFileAndSettings,
  CertificationServiceSettings,
  OcspResponder,
} from '@/openapi-types';
import { defineStore } from 'pinia';
import axios from 'axios';

export interface CertificationServiceStoreState {
  certificationServices: ApprovedCertificationServiceListItem[];
  currentCertificationService: ApprovedCertificationService | null;
}

export const useCertificationServiceStore = defineStore(
  'certificationService',
  {
    state: (): CertificationServiceStoreState => ({
      certificationServices: [],
      currentCertificationService: null,
    }),
    persist: true,
    actions: {
      fetchAll() {
        return axios
          .get<ApprovedCertificationServiceListItem[]>(
            '/certification-services',
          )
          .then((resp) => (this.certificationServices = resp.data));
      },
      loadById(certificationServiceId: number) {
        return axios
          .get<ApprovedCertificationService>(
            `/certification-services/${certificationServiceId}`,
          )
          .then((resp) => {
            this.currentCertificationService = resp.data;
          })
          .catch((error) => {
            throw error;
          });
      },
      add(newCas: CertificationServiceFileAndSettings) {
        const formData = new FormData();
        formData.append(
          'certificate_profile_info',
          newCas.certificate_profile_info || '',
        );
        formData.append('tls_auth', newCas.tls_auth || '');
        formData.append('certificate', newCas.certificate);
        return axios
          .post('/certification-services', formData)
          .finally(() => this.fetchAll());
      },
      update(certificationServiceId: number, settings: CertificationServiceSettings) {
        return axios
          .patch<ApprovedCertificationService>(`/certification-services/${certificationServiceId}`, settings)
          .then((resp) => {
            this.currentCertificationService = resp.data;
          })
          .catch((error) => {
            throw error;
          });
      },
      getCertificate(certificationServiceId: number) {
        return axios.get<CertificateDetails>(`/certification-services/${certificationServiceId}/certificate`)
      }
    },
  },
);


export interface OcspResponderStoreState {
  currentCa: ApprovedCertificationService | CertificateAuthority | null
  currentOcspResponders: OcspResponder[];
}

export const useOcspResponderStore = defineStore(
  'ocspResponderService',
  {
    state: (): OcspResponderStoreState => ({
      currentCa: null,
      currentOcspResponders: [],
    }),
    persist: true,
    actions: {
      loadByCa(currentCa: ApprovedCertificationService | CertificateAuthority) {
        this.currentCa = currentCa;
        this.fetchOcspResponders();
      },
      fetchOcspResponders() {
        if (!this.currentCa) return

        return axios
          .get<OcspResponder[]>(
            `/certification-services/${this.currentCa.id}/ocsp-responders`,
          )
          .then((resp) => (this.currentOcspResponders = resp.data));
      },
      addOcspResponder(url: string, certificate: File) {
        const formData = new FormData();
        formData.append('url', url);
        formData.append('certificate', certificate);
        return axios
          .post(`/certification-services/${this.currentCa!.id}/ocsp-responders`, formData)
          .finally(() => this.fetchOcspResponders());
      },
      updateOcspResponder(id: number, url: string, certificate: File | null) {
        const formData = new FormData();
        formData.append('url', url);
        if (certificate) {
          formData.append('certificate', certificate);
        }
        return axios
          .patch(`/ocsp-responders/${id}/`, formData);
      },
      deleteOcspResponder(id: number) {
        return axios.delete(`/ocsp-responders/${id}`);
      },
      getOcspResponderCertificate(id: number) {
        return axios.get<CertificateDetails>(`/ocsp-responders/${id}/certificate`)
      }
    },
  },
);

export interface IntermediateCasStoreState {
  currentCa: ApprovedCertificationService | CertificateAuthority | null
  currentIntermediateCas: CertificateAuthority[];
}

export const useIntermediateCaStore = defineStore(
    'intermediateCasService',
    {
      state: (): IntermediateCasStoreState => ({
        currentCa: null,
        currentIntermediateCas: [],
      }),
      persist: true,
      actions: {
        loadByCa(currentCa: ApprovedCertificationService | CertificateAuthority) {
          this.currentCa = currentCa;
          this.fetchIntermediateCas();
        },
        fetchIntermediateCas() {
          if (!this.currentCa) return

          return axios
              .get<CertificateAuthority[]>(
                  `/certification-services/${this.currentCa.id}/intermediate-cas`,
              )
              .then((resp) => (this.currentIntermediateCas = resp.data));
        },
        getIntermediateCa(id: number) {
          return axios.get<CertificateAuthority>(`/intermediate-cas/${id}`)
        },
        addIntermediateCa(certificate: File) {
          if (!this.currentCa) {
            throw new Error('CA not selected');
          }
          const formData = new FormData();
          formData.append('certificate', certificate);
          return axios
              .post(`/certification-services/${this.currentCa.id}/intermediate-cas`, formData)
              .finally(() => this.fetchIntermediateCas());
        },
        deleteIntermediateCa(id: number) {
          return axios.delete(`/intermediate-cas/${id}`);
        }
      },
    },
);
