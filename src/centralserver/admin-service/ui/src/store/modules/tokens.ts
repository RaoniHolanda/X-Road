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

import { defineStore } from 'pinia';

import {
  Token,
  TokenType,
  TokenStatus,
  KeyUsageType,
} from '@/mock-openapi-types';
import { deepClone } from '@/util/helpers';

export const tokenStore = defineStore('tokenStore', {
  state: () => {
    return {
      expandedTokens: [] as string[],
      tokens: [
        {
          id: '87768768678768',
          name: 'this is a token name',
          type: TokenType.SOFTWARE,
          keys: [
            {
              id: 'sdfsdf9384',
              name: 'keyone',
              label: 'ready label one',
              certificates: [],
              certificate_signing_requests: [],
              usage: KeyUsageType.SIGNING,
            },
            {
              id: '32123123321',
              name: 'keytwo',
              label: 'ready label two',
              certificates: [],
              certificate_signing_requests: [],
              usage: KeyUsageType.SIGNING,
            },
          ], // Array<Key>;
          status: TokenStatus.USER_PIN_EXPIRED,
          logged_in: false,
          available: true,
          saved_to_configuration: true,
          read_only: false,
          serial_number: 'jdjhkfjhkdfs',
        },
        {
          id: '09896745443678768',
          name: 'kuuioi',
          type: TokenType.SOFTWARE,
          keys: [
            {
              id: '323223232',
              name: 'key3',
              label: 'ready label 3',
              certificates: [],
              certificate_signing_requests: [],
              usage: KeyUsageType.SIGNING,
            },
          ],
          status: TokenStatus.USER_PIN_EXPIRED,
          logged_in: true,
          available: true,
          saved_to_configuration: true,
          read_only: false,
          serial_number: 'yfjhgjghgfhs',
        },
      ] as Token[],
      selectedToken: undefined as Token | undefined,

      count: 20,
    };
  },
  getters: {
    tokenExpanded: (state) => (id: string) => {
      return state.expandedTokens.includes(id);
    },

    getTokens(state) {
      return state.tokens;
    },

    getSortedTokens(state) {
      if (!state.tokens || state.tokens.length === 0) {
        return [];
      }

      // Sort array by id:s so it doesn't jump around. Order of items in the backend reply changes between requests.
      const arr = deepClone(state.tokens).sort((a, b) => {
        if (a.id < b.id) {
          return -1;
        }
        if (a.id > b.id) {
          return 1;
        }

        // equal id:s. (should not happen)
        return 0;
      });

      return arr;
    },
    getSelectedToken(state) {
      return state.selectedToken;
    },
  },

  actions: {
    setTokenHidden(id: string) {
      const index = this.expandedTokens.findIndex((element) => {
        return element === id;
      });

      if (index >= 0) {
        this.expandedTokens.splice(index, 1);
      }
    },

    setTokenExpanded(id: string) {
      const index = this.expandedTokens.findIndex((element) => {
        return element === id;
      });

      if (index === -1) {
        this.expandedTokens.push(id);
      }
    },

    setTokens(tokens: Token[]) {
      this.tokens = tokens;
    },

    setSelectedToken(token: Token) {
      this.selectedToken = token;
    },
  },
});
