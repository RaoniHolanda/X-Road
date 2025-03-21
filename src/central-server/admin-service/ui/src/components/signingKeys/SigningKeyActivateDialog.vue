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
  <xrd-confirm-dialog
    :dialog="true"
    :loading="loading"
    :data="label"
    accept-button-text="keys.dialog.activate.confirmButton"
    title="keys.dialog.activate.title"
    text="keys.dialog.activate.text"
    @cancel="cancel"
    @accept="confirmActivate"
  />
</template>

<script lang="ts">
import Vue from 'vue';
import { mapActions, mapStores } from 'pinia';
import { useNotifications } from '@/store/modules/notifications';
import { useSigningKey } from '@/store/modules/signing-keys';
import { ConfigurationSigningKey } from '@/openapi-types';
import { Prop } from 'vue/types/options';

export default Vue.extend({
  props: {
    signingKey: {
      type: Object as Prop<ConfigurationSigningKey>,
      required: true,
    },
  },
  data() {
    return {
      loading: false,
    };
  },
  computed: {
    ...mapStores(useSigningKey),
    label() {
      const key: ConfigurationSigningKey = this.signingKey;
      return { label: key?.label?.label || key.id };
    },
  },
  methods: {
    ...mapActions(useNotifications, ['showError', 'showSuccess']),
    cancel(): void {
      this.$emit('cancel');
    },
    confirmActivate(): void {
      this.loading = true;
      this.signingKeyStore
        .activateSigningKey(this.signingKey.id)
        .then(() => {
          this.showSuccess(this.$t('keys.dialog.activate.success', this.label));
          this.$emit('key-activate');
        })
        .catch((error) => {
          this.showError(error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
});
</script>

<style lang="scss" scoped></style>
