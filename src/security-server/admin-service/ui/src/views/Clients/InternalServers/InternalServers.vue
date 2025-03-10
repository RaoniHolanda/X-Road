<!--
   The MIT License
   Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
   Copyright (c) 2018 Estonian Information System Authority (RIA),
   Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
   Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
   THE SOFTWARE.
 -->
<template>
  <div>
    <v-card v-if="showConnectionType" flat class="xrd-card">
      <v-flex class="px-4 pt-4">
        <h1 class="title mb-3">{{ $t('internalServers.connectionType') }}</h1>
        <v-select
          :key="revertHack"
          v-model="connectionTypeModel"
          :items="connectionTypes"
          class="select-connection"
          outlined
          :disabled="!canEditConnectionType"
          :readonly="!canEditConnectionType"
        ></v-select>
      </v-flex>
      <div class="conn-info pa-4">
        {{ $t('internalServers.connectionInfo') }}
      </div>
    </v-card>

    <v-card flat class="xrd-card pb-4">
      <div class="tls-title-wrap pa-4">
        <h1 class="title mb-3">{{ $t('internalServers.tlsTitle') }}</h1>
        <xrd-file-upload
          v-if="canAddTlsCert"
          v-slot="{ upload }"
          accepts=".pem, .cer, .der"
          @file-changed="onFileChange"
        >
          <xrd-button outlined color="primary" @click="upload"
            ><v-icon class="xrd-large-button-icon">icon-Add</v-icon
            >{{ $t('action.add') }}</xrd-button
          >
        </xrd-file-upload>
      </div>
      <div class="cert-table-title pl-4">
        {{ $t('internalServers.certHash') }}
      </div>
      <table class="server-certificates xrd-table">
        <template v-if="tlsCertificates && tlsCertificates.length > 0">
          <tr v-for="certificate in tlsCertificates" :key="certificate.hash">
            <td class="pl-4 pt-2">
              <i class="icon-Certificate icon" />
            </td>
            <td>
              <span
                v-if="canViewTlsCertDetails"
                class="certificate-link"
                @click="openCertificate(certificate)"
                >{{ certificate.hash | colonize }}</span
              >
              <span v-else>{{ certificate.hash | colonize }}</span>
            </td>
          </tr>
        </template>

        <XrdEmptyPlaceholderRow
          :colspan="2"
          :loading="tlsCertLoading"
          :data="tlsCertificates"
          :no-items-text="$t('noData.noCertificates')"
        />
      </table>
    </v-card>

    <v-card v-if="canViewSSCert" flat class="xrd-card pb-4">
      <div class="pa-4">
        <h1 class="title mb-3">{{ $t('internalServers.ssCertTitle') }}</h1>
      </div>
      <div class="cert-table-title pl-4">
        {{ $t('internalServers.certHash') }}
      </div>
      <table class="server-certificates xrd-table">
        <template v-if="ssCertificate && !ssCertLoading">
          <tr>
            <td class="pl-4 pt-2">
              <i class="icon-Certificate icon" />
            </td>
            <td>
              <span>{{ ssCertificate.hash | colonize }}</span>
            </td>

            <td class="column-button">
              <xrd-button
                v-if="canExportSSCert"
                small
                :outlined="false"
                text
                color="primary"
                @click="exportSSCertificate"
                >{{ $t('action.export') }}</xrd-button
              >
            </td>
          </tr>
        </template>
        <XrdEmptyPlaceholderRow
          :colspan="3"
          :loading="ssCertLoading"
          :data="ssCertificate"
          :no-items-text="$t('noData.noCertificate')"
        />
      </table>
    </v-card>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';

import { Permissions, RouteName } from '@/global';
import { FileUploadResult } from '@niis/shared-ui';
import { CertificateDetails } from '@/openapi-types';
import { saveResponseAsFile } from '@/util/helpers';
import * as api from '@/util/api';
import { encodePathParameter } from '@/util/api';
import { mapActions, mapState } from 'pinia';
import { useNotifications } from '@/store/modules/notifications';
import { useUser } from '@/store/modules/user';
import { useClient } from '@/store/modules/client';

export default Vue.extend({
  props: {
    id: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      connectionTypes: [
        { text: 'HTTP', value: 'HTTP' },
        { text: 'HTTPS', value: 'HTTPS' },
        { text: 'HTTPS NO AUTH', value: 'HTTPS_NO_AUTH' },
      ],
      dialog: false,
      selectedCertificate: null,
      revertHack: 0,
      tlsCertLoading: false,
      ssCertLoading: false,
    };
  },
  computed: {
    ...mapState(useUser, ['hasPermission']),
    ...mapState(useClient, [
      'tlsCertificates',
      'ssCertificate',
      'connectionType',
    ]),

    connectionTypeModel: {
      get(): string | undefined | null {
        return this.connectionType;
      },
      set(value: string) {
        this.saveConnectionType({
          clientId: this.id,
          connType: value,
        })
          .then(() => {
            this.showSuccess(this.$t('internalServers.connTypeUpdated'));
          })
          .catch((error) => {
            this.revertHack += 1;
            this.showError(error);
          });
      },
    },

    showConnectionType(): boolean {
      return this.hasPermission(
        Permissions.VIEW_CLIENT_INTERNAL_CONNECTION_TYPE,
      );
    },
    canEditConnectionType(): boolean {
      return this.hasPermission(
        Permissions.EDIT_CLIENT_INTERNAL_CONNECTION_TYPE,
      );
    },
    canViewTlsCertDetails(): boolean {
      return this.hasPermission(Permissions.VIEW_CLIENT_INTERNAL_CERT_DETAILS);
    },
    canAddTlsCert(): boolean {
      return this.hasPermission(Permissions.ADD_CLIENT_INTERNAL_CERT);
    },
    canViewSSCert(): boolean {
      return this.hasPermission(Permissions.VIEW_INTERNAL_TLS_CERT);
    },
    canExportSSCert(): boolean {
      return this.hasPermission(Permissions.EXPORT_INTERNAL_TLS_CERT);
    },
  },
  created() {
    this.fetchSSCert(this.id);
    this.fetchTlsCerts(this.id);
  },
  methods: {
    ...mapActions(useNotifications, ['showError', 'showSuccess']),
    ...mapActions(useClient, [
      'saveConnectionType',
      'fetchTlsCertificates',
      'fetchSSCertificate',
    ]),
    onFileChange(event: FileUploadResult): void {
      api
        .post(
          `/clients/${encodePathParameter(this.id)}/tls-certificates`,
          event.buffer,
          {
            headers: {
              'Content-Type': 'application/octet-stream',
            },
          },
        )
        .then(
          () => {
            // Refresh the tls cert list
            this.fetchTlsCerts(this.id);
          },
          (error) => {
            this.showError(error);
          },
        );
    },

    fetchTlsCerts(id: string): void {
      this.tlsCertLoading = true;
      this.fetchTlsCertificates(id)
        .catch((error) => {
          this.showError(error);
        })
        .finally(() => (this.tlsCertLoading = false));
    },

    exportSSCertificate(): void {
      api
        .get('/system/certificate/export', { responseType: 'arraybuffer' })
        .then((response) => {
          saveResponseAsFile(response);
        })
        .catch((error) => {
          this.showError(error);
        });
    },

    fetchSSCert(id: string): void {
      this.ssCertLoading = true;
      this.fetchSSCertificate(id)
        .catch((error) => {
          this.showError(error);
        })
        .finally(() => (this.ssCertLoading = false));
    },

    openCertificate(cert: CertificateDetails): void {
      this.$router.push({
        name: RouteName.ClientTlsCertificate,
        params: {
          id: this.id,
          hash: cert.hash,
        },
      });
    },
    closeDialog(): void {
      this.dialog = false;
    },
  },
});
</script>

<style lang="scss" scoped>
@import '~styles/tables';
@import '~styles/colors';

.select-connection {
  max-width: 240px;
}

.tls-title-wrap {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.xrd-card {
  margin-top: 40px;
}

.conn-info {
  color: $XRoad-Black70;
}

.cert-table-title {
  color: $XRoad-Black70;
  font-size: $XRoad-DefaultFontSize;
  font-weight: bold;
  margin: 5px;
}

.server-certificates {
  width: 100%;
  border-top: $XRoad-WarmGrey30 solid 1px;
}

.icon {
  width: 2px;
}

.column-button {
  text-align: end;
}

.certificate-link {
  text-decoration: underline;
  cursor: pointer;
}
</style>
