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
  <xrd-button
    v-if="canDelete"
    data-test="delete-anchor-button"
    outlined
    @click="$refs.dialog.open()"
  >
    {{ $t('action.delete') }}
    <delete-trusted-anchor-dialog
      ref="dialog"
      :hash="hash"
      :identifier="identifier"
      @deleted="$emit('deleted')"
    />
  </xrd-button>
</template>
<script lang="ts">
import Vue from 'vue';
import { mapState } from 'pinia';
import { useUser } from '@/store/modules/user';
import { Permissions } from '@/global';
import DeleteTrustedAnchorDialog from './DeleteTrustedAnchorDialog.vue';

export default Vue.extend({
  components: { DeleteTrustedAnchorDialog },
  props: {
    hash: {
      type: String,
      required: true,
    },
    identifier: {
      type: String,
      required: true,
    },
  },
  computed: {
    ...mapState(useUser, ['hasPermission']),
    canDelete(): boolean {
      return this.hasPermission(Permissions.DELETE_TRUSTED_ANCHOR);
    },
  },
});
</script>
<style lang="scss" scoped></style>
