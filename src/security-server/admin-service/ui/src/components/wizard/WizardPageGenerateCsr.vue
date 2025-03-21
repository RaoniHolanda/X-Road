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
    <ValidationObserver ref="form2" v-slot="{ invalid }">
      <div class="wizard-step-form-content">
        <div v-for="item in csrForm" :key="item.id" class="wizard-row-wrap">
          <div class="wizard-label">
            {{ $t(`certificateProfile.${item.label_key}`) }}
          </div>

          <div>
            <ValidationProvider
              v-slot="{ errors }"
              :name="item.id"
              :rules="item.required && 'required'"
            >
              <v-text-field
                v-model="item.default_value"
                class="wizard-form-input"
                :name="item.id"
                type="text"
                outlined
                :disabled="item.read_only"
                :error-messages="errors"
                data-test="dynamic-csr-input"
                autofocus
              ></v-text-field>
            </ValidationProvider>
          </div>
        </div>
        <div class="generate-row">
          <div>{{ $t('csr.saveInfo') }}</div>
          <xrd-button
            :disabled="invalid || !disableDone"
            data-test="generate-csr-button"
            @click="generateCsr"
            >{{ $t('csr.generateCsr') }}</xrd-button
          >
        </div>
      </div>
      <div class="button-footer">
        <xrd-button
          outlined
          :disabled="!disableDone"
          data-test="cancel-button"
          @click="cancel"
          >{{ $t('action.cancel') }}</xrd-button
        >

        <xrd-button
          outlined
          class="previous-button"
          data-test="previous-button"
          :disabled="!disableDone"
          @click="previous"
          >{{ $t('action.previous') }}</xrd-button
        >
        <xrd-button
          :disabled="disableDone"
          data-test="save-button"
          @click="done"
          >{{ $t(saveButtonText) }}</xrd-button
        >
      </div>
    </ValidationObserver>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { ValidationProvider, ValidationObserver } from 'vee-validate';
import { mapActions, mapState } from 'pinia';
import { useNotifications } from '@/store/modules/notifications';
import { useCsr } from '@/store/modules/certificateSignRequest';
import { AxiosError } from 'axios';

export default Vue.extend({
  components: {
    ValidationObserver,
    ValidationProvider,
  },
  props: {
    saveButtonText: {
      type: String,
      default: 'action.done',
    },
    // Creating Key + CSR or just CSR
    keyAndCsr: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      disableDone: true,
    };
  },
  computed: {
    ...mapState(useCsr, ['csrForm', 'csrTokenId']),
  },
  methods: {
    ...mapActions(useNotifications, ['showError', 'showSuccess']),
    ...mapActions(useCsr, ['requestGenerateCsr', 'generateKeyAndCsr']),
    cancel(): void {
      this.$emit('cancel');
    },
    previous(): void {
      this.$emit('previous');
    },
    done(): void {
      this.$emit('done');
    },
    async generateCsr(): Promise<void> {
      if (this.keyAndCsr) {
        // Create Key AND CSR

        if (!this.csrTokenId) {
          // Should not happen
          throw new Error('Token id does not exist');
        }
        const tokenId = this.csrTokenId;

        // Create key and CSR
        try {
          await this.generateKeyAndCsr(tokenId);
          this.disableDone = false;
        } catch (error) {
          this.disableDone = true;
          // Error comes from axios, so it most probably is AxiosError
          this.showError(error as AxiosError);
        }
      } else {
        // Create only CSR
        try {
          await this.requestGenerateCsr();
          this.disableDone = false;
        } catch (error) {
          this.disableDone = true;
          // Error comes from axios, so it most probably is AxiosError
          this.showError(error as AxiosError);
        }
      }
    },
  },
});
</script>

<style lang="scss" scoped>
@import '../../assets/wizards';

.generate-row {
  margin-top: 40px;
  width: 840px;
  display: flex;
  flex-direction: row;
  align-items: baseline;
  justify-content: space-between;
}
</style>
