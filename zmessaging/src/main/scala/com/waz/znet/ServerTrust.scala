/*
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.znet

import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.{X509TrustManager, TrustManagerFactory}

import com.waz.ZLog
import com.waz.utils.returning

object ServerTrust {

  private val digiCertGlobalRootCA: Array[Byte] = Array(
    0x30,0x82,0x03,0xaf,0x30,0x82,0x02,0x97,0xa0,0x03,0x02,0x01,0x02,0x02,0x10,0x08,0x3b,0xe0,0x56,0x90,
    0x42,0x46,0xb1,0xa1,0x75,0x6a,0xc9,0x59,0x91,0xc7,0x4a,0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,
    0x0d,0x01,0x01,0x05,0x05,0x00,0x30,0x61,0x31,0x0b,0x30,0x09,0x06,0x03,0x55,0x04,0x06,0x13,0x02,0x55,
    0x53,0x31,0x15,0x30,0x13,0x06,0x03,0x55,0x04,0x0a,0x13,0x0c,0x44,0x69,0x67,0x69,0x43,0x65,0x72,0x74,
    0x20,0x49,0x6e,0x63,0x31,0x19,0x30,0x17,0x06,0x03,0x55,0x04,0x0b,0x13,0x10,0x77,0x77,0x77,0x2e,0x64,
    0x69,0x67,0x69,0x63,0x65,0x72,0x74,0x2e,0x63,0x6f,0x6d,0x31,0x20,0x30,0x1e,0x06,0x03,0x55,0x04,0x03,
    0x13,0x17,0x44,0x69,0x67,0x69,0x43,0x65,0x72,0x74,0x20,0x47,0x6c,0x6f,0x62,0x61,0x6c,0x20,0x52,0x6f,
    0x6f,0x74,0x20,0x43,0x41,0x30,0x1e,0x17,0x0d,0x30,0x36,0x31,0x31,0x31,0x30,0x30,0x30,0x30,0x30,0x30,
    0x30,0x5a,0x17,0x0d,0x33,0x31,0x31,0x31,0x31,0x30,0x30,0x30,0x30,0x30,0x30,0x30,0x5a,0x30,0x61,0x31,
    0x0b,0x30,0x09,0x06,0x03,0x55,0x04,0x06,0x13,0x02,0x55,0x53,0x31,0x15,0x30,0x13,0x06,0x03,0x55,0x04,
    0x0a,0x13,0x0c,0x44,0x69,0x67,0x69,0x43,0x65,0x72,0x74,0x20,0x49,0x6e,0x63,0x31,0x19,0x30,0x17,0x06,
    0x03,0x55,0x04,0x0b,0x13,0x10,0x77,0x77,0x77,0x2e,0x64,0x69,0x67,0x69,0x63,0x65,0x72,0x74,0x2e,0x63,
    0x6f,0x6d,0x31,0x20,0x30,0x1e,0x06,0x03,0x55,0x04,0x03,0x13,0x17,0x44,0x69,0x67,0x69,0x43,0x65,0x72,
    0x74,0x20,0x47,0x6c,0x6f,0x62,0x61,0x6c,0x20,0x52,0x6f,0x6f,0x74,0x20,0x43,0x41,0x30,0x82,0x01,0x22,
    0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,0x0d,0x01,0x01,0x01,0x05,0x00,0x03,0x82,0x01,0x0f,0x00,
    0x30,0x82,0x01,0x0a,0x02,0x82,0x01,0x01,0x00,0xe2,0x3b,0xe1,0x11,0x72,0xde,0xa8,0xa4,0xd3,0xa3,0x57,
    0xaa,0x50,0xa2,0x8f,0x0b,0x77,0x90,0xc9,0xa2,0xa5,0xee,0x12,0xce,0x96,0x5b,0x01,0x09,0x20,0xcc,0x01,
    0x93,0xa7,0x4e,0x30,0xb7,0x53,0xf7,0x43,0xc4,0x69,0x00,0x57,0x9d,0xe2,0x8d,0x22,0xdd,0x87,0x06,0x40,
    0x00,0x81,0x09,0xce,0xce,0x1b,0x83,0xbf,0xdf,0xcd,0x3b,0x71,0x46,0xe2,0xd6,0x66,0xc7,0x05,0xb3,0x76,
    0x27,0x16,0x8f,0x7b,0x9e,0x1e,0x95,0x7d,0xee,0xb7,0x48,0xa3,0x08,0xda,0xd6,0xaf,0x7a,0x0c,0x39,0x06,
    0x65,0x7f,0x4a,0x5d,0x1f,0xbc,0x17,0xf8,0xab,0xbe,0xee,0x28,0xd7,0x74,0x7f,0x7a,0x78,0x99,0x59,0x85,
    0x68,0x6e,0x5c,0x23,0x32,0x4b,0xbf,0x4e,0xc0,0xe8,0x5a,0x6d,0xe3,0x70,0xbf,0x77,0x10,0xbf,0xfc,0x01,
    0xf6,0x85,0xd9,0xa8,0x44,0x10,0x58,0x32,0xa9,0x75,0x18,0xd5,0xd1,0xa2,0xbe,0x47,0xe2,0x27,0x6a,0xf4,
    0x9a,0x33,0xf8,0x49,0x08,0x60,0x8b,0xd4,0x5f,0xb4,0x3a,0x84,0xbf,0xa1,0xaa,0x4a,0x4c,0x7d,0x3e,0xcf,
    0x4f,0x5f,0x6c,0x76,0x5e,0xa0,0x4b,0x37,0x91,0x9e,0xdc,0x22,0xe6,0x6d,0xce,0x14,0x1a,0x8e,0x6a,0xcb,
    0xfe,0xcd,0xb3,0x14,0x64,0x17,0xc7,0x5b,0x29,0x9e,0x32,0xbf,0xf2,0xee,0xfa,0xd3,0x0b,0x42,0xd4,0xab,
    0xb7,0x41,0x32,0xda,0x0c,0xd4,0xef,0xf8,0x81,0xd5,0xbb,0x8d,0x58,0x3f,0xb5,0x1b,0xe8,0x49,0x28,0xa2,
    0x70,0xda,0x31,0x04,0xdd,0xf7,0xb2,0x16,0xf2,0x4c,0x0a,0x4e,0x07,0xa8,0xed,0x4a,0x3d,0x5e,0xb5,0x7f,
    0xa3,0x90,0xc3,0xaf,0x27,0x02,0x03,0x01,0x00,0x01,0xa3,0x63,0x30,0x61,0x30,0x0e,0x06,0x03,0x55,0x1d,
    0x0f,0x01,0x01,0xff,0x04,0x04,0x03,0x02,0x01,0x86,0x30,0x0f,0x06,0x03,0x55,0x1d,0x13,0x01,0x01,0xff,
    0x04,0x05,0x30,0x03,0x01,0x01,0xff,0x30,0x1d,0x06,0x03,0x55,0x1d,0x0e,0x04,0x16,0x04,0x14,0x03,0xde,
    0x50,0x35,0x56,0xd1,0x4c,0xbb,0x66,0xf0,0xa3,0xe2,0x1b,0x1b,0xc3,0x97,0xb2,0x3d,0xd1,0x55,0x30,0x1f,
    0x06,0x03,0x55,0x1d,0x23,0x04,0x18,0x30,0x16,0x80,0x14,0x03,0xde,0x50,0x35,0x56,0xd1,0x4c,0xbb,0x66,
    0xf0,0xa3,0xe2,0x1b,0x1b,0xc3,0x97,0xb2,0x3d,0xd1,0x55,0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,
    0x0d,0x01,0x01,0x05,0x05,0x00,0x03,0x82,0x01,0x01,0x00,0xcb,0x9c,0x37,0xaa,0x48,0x13,0x12,0x0a,0xfa,
    0xdd,0x44,0x9c,0x4f,0x52,0xb0,0xf4,0xdf,0xae,0x04,0xf5,0x79,0x79,0x08,0xa3,0x24,0x18,0xfc,0x4b,0x2b,
    0x84,0xc0,0x2d,0xb9,0xd5,0xc7,0xfe,0xf4,0xc1,0x1f,0x58,0xcb,0xb8,0x6d,0x9c,0x7a,0x74,0xe7,0x98,0x29,
    0xab,0x11,0xb5,0xe3,0x70,0xa0,0xa1,0xcd,0x4c,0x88,0x99,0x93,0x8c,0x91,0x70,0xe2,0xab,0x0f,0x1c,0xbe,
    0x93,0xa9,0xff,0x63,0xd5,0xe4,0x07,0x60,0xd3,0xa3,0xbf,0x9d,0x5b,0x09,0xf1,0xd5,0x8e,0xe3,0x53,0xf4,
    0x8e,0x63,0xfa,0x3f,0xa7,0xdb,0xb4,0x66,0xdf,0x62,0x66,0xd6,0xd1,0x6e,0x41,0x8d,0xf2,0x2d,0xb5,0xea,
    0x77,0x4a,0x9f,0x9d,0x58,0xe2,0x2b,0x59,0xc0,0x40,0x23,0xed,0x2d,0x28,0x82,0x45,0x3e,0x79,0x54,0x92,
    0x26,0x98,0xe0,0x80,0x48,0xa8,0x37,0xef,0xf0,0xd6,0x79,0x60,0x16,0xde,0xac,0xe8,0x0e,0xcd,0x6e,0xac,
    0x44,0x17,0x38,0x2f,0x49,0xda,0xe1,0x45,0x3e,0x2a,0xb9,0x36,0x53,0xcf,0x3a,0x50,0x06,0xf7,0x2e,0xe8,
    0xc4,0x57,0x49,0x6c,0x61,0x21,0x18,0xd5,0x04,0xad,0x78,0x3c,0x2c,0x3a,0x80,0x6b,0xa7,0xeb,0xaf,0x15,
    0x14,0xe9,0xd8,0x89,0xc1,0xb9,0x38,0x6c,0xe2,0x91,0x6c,0x8a,0xff,0x64,0xb9,0x77,0x25,0x57,0x30,0xc0,
    0x1b,0x24,0xa3,0xe1,0xdc,0xe9,0xdf,0x47,0x7c,0xb5,0xb4,0x24,0x08,0x05,0x30,0xec,0x2d,0xbd,0x0b,0xbf,
    0x45,0xbf,0x50,0xb9,0xa9,0xf3,0xeb,0x98,0x01,0x12,0xad,0xc8,0x88,0xc6,0x98,0x34,0x5f,0x8d,0x0a,0x3c,
    0xc6,0xe9,0xd5,0x95,0x95,0x6d,0xde) .map (_.toByte)

  private val cdnRootCA: Array[Byte]= Array(
    0x30,0x82,0x02,0x3c,0x30,0x82,0x01,0xa5,0x02,0x10,0x70,0xba,0xe4,0x1d,0x10,0xd9,0x29,0x34,0xb6,0x38,
    0xca,0x7b,0x03,0xcc,0xba,0xbf,0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,0x0d,0x01,0x01,0x02,0x05,
    0x00,0x30,0x5f,0x31,0x0b,0x30,0x09,0x06,0x03,0x55,0x04,0x06,0x13,0x02,0x55,0x53,0x31,0x17,0x30,0x15,
    0x06,0x03,0x55,0x04,0x0a,0x13,0x0e,0x56,0x65,0x72,0x69,0x53,0x69,0x67,0x6e,0x2c,0x20,0x49,0x6e,0x63,
    0x2e,0x31,0x37,0x30,0x35,0x06,0x03,0x55,0x04,0x0b,0x13,0x2e,0x43,0x6c,0x61,0x73,0x73,0x20,0x33,0x20,
    0x50,0x75,0x62,0x6c,0x69,0x63,0x20,0x50,0x72,0x69,0x6d,0x61,0x72,0x79,0x20,0x43,0x65,0x72,0x74,0x69,
    0x66,0x69,0x63,0x61,0x74,0x69,0x6f,0x6e,0x20,0x41,0x75,0x74,0x68,0x6f,0x72,0x69,0x74,0x79,0x30,0x1e,
    0x17,0x0d,0x39,0x36,0x30,0x31,0x32,0x39,0x30,0x30,0x30,0x30,0x30,0x30,0x5a,0x17,0x0d,0x32,0x38,0x30,
    0x38,0x30,0x31,0x32,0x33,0x35,0x39,0x35,0x39,0x5a,0x30,0x5f,0x31,0x0b,0x30,0x09,0x06,0x03,0x55,0x04,
    0x06,0x13,0x02,0x55,0x53,0x31,0x17,0x30,0x15,0x06,0x03,0x55,0x04,0x0a,0x13,0x0e,0x56,0x65,0x72,0x69,
    0x53,0x69,0x67,0x6e,0x2c,0x20,0x49,0x6e,0x63,0x2e,0x31,0x37,0x30,0x35,0x06,0x03,0x55,0x04,0x0b,0x13,
    0x2e,0x43,0x6c,0x61,0x73,0x73,0x20,0x33,0x20,0x50,0x75,0x62,0x6c,0x69,0x63,0x20,0x50,0x72,0x69,0x6d,
    0x61,0x72,0x79,0x20,0x43,0x65,0x72,0x74,0x69,0x66,0x69,0x63,0x61,0x74,0x69,0x6f,0x6e,0x20,0x41,0x75,
    0x74,0x68,0x6f,0x72,0x69,0x74,0x79,0x30,0x81,0x9f,0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,0x0d,
    0x01,0x01,0x01,0x05,0x00,0x03,0x81,0x8d,0x00,0x30,0x81,0x89,0x02,0x81,0x81,0x00,0xc9,0x5c,0x59,0x9e,
    0xf2,0x1b,0x8a,0x01,0x14,0xb4,0x10,0xdf,0x04,0x40,0xdb,0xe3,0x57,0xaf,0x6a,0x45,0x40,0x8f,0x84,0x0c,
    0x0b,0xd1,0x33,0xd9,0xd9,0x11,0xcf,0xee,0x02,0x58,0x1f,0x25,0xf7,0x2a,0xa8,0x44,0x05,0xaa,0xec,0x03,
    0x1f,0x78,0x7f,0x9e,0x93,0xb9,0x9a,0x00,0xaa,0x23,0x7d,0xd6,0xac,0x85,0xa2,0x63,0x45,0xc7,0x72,0x27,
    0xcc,0xf4,0x4c,0xc6,0x75,0x71,0xd2,0x39,0xef,0x4f,0x42,0xf0,0x75,0xdf,0x0a,0x90,0xc6,0x8e,0x20,0x6f,
    0x98,0x0f,0xf8,0xac,0x23,0x5f,0x70,0x29,0x36,0xa4,0xc9,0x86,0xe7,0xb1,0x9a,0x20,0xcb,0x53,0xa5,0x85,
    0xe7,0x3d,0xbe,0x7d,0x9a,0xfe,0x24,0x45,0x33,0xdc,0x76,0x15,0xed,0x0f,0xa2,0x71,0x64,0x4c,0x65,0x2e,
    0x81,0x68,0x45,0xa7,0x02,0x03,0x01,0x00,0x01,0x30,0x0d,0x06,0x09,0x2a,0x86,0x48,0x86,0xf7,0x0d,0x01,
    0x01,0x02,0x05,0x00,0x03,0x81,0x81,0x00,0xbb,0x4c,0x12,0x2b,0xcf,0x2c,0x26,0x00,0x4f,0x14,0x13,0xdd,
    0xa6,0xfb,0xfc,0x0a,0x11,0x84,0x8c,0xf3,0x28,0x1c,0x67,0x92,0x2f,0x7c,0xb6,0xc5,0xfa,0xdf,0xf0,0xe8,
    0x95,0xbc,0x1d,0x8f,0x6c,0x2c,0xa8,0x51,0xcc,0x73,0xd8,0xa4,0xc0,0x53,0xf0,0x4e,0xd6,0x26,0xc0,0x76,
    0x01,0x57,0x81,0x92,0x5e,0x21,0xf1,0xd1,0xb1,0xff,0xe7,0xd0,0x21,0x58,0xcd,0x69,0x17,0xe3,0x44,0x1c,
    0x9c,0x19,0x44,0x39,0x89,0x5c,0xdc,0x9c,0x00,0x0f,0x56,0x8d,0x02,0x99,0xed,0xa2,0x90,0x45,0x4c,0xe4,
    0xbb,0x10,0xa4,0x3d,0xf0,0x32,0x03,0x0e,0xf1,0xce,0xf8,0xe8,0xc9,0x51,0x8c,0xe6,0x62,0x9f,0xe6,0x9f,
    0xc0,0x7d,0xb7,0x72,0x9c,0xc9,0x36,0x3a,0x6b,0x9f,0x4e,0xa8,0xff,0x64,0x0d,0x64) .map (_.toByte)

  val systemTrustManager: X509TrustManager = trustManagerForTrustStore(null) // use the system keystore
  val cdnTrustManager: X509TrustManager = trustManager(cdnRootCA)
  val backendTrustManager: X509TrustManager = trustManager(digiCertGlobalRootCA)

  def trustManager(bytes: Array[Byte]): X509TrustManager = {
    val in = new ByteArrayInputStream(bytes)
    val ca = try CertificateFactory.getInstance("X.509").generateCertificate(in) finally in.close()
    trustManagerForTrustStore(returning(KeyStore.getInstance(KeyStore.getDefaultType)) { store =>
      store.load(null, null)
      store.setCertificateEntry("ca", ca)
    })
  }

  def trustManagerForTrustStore(trustStore: KeyStore): X509TrustManager =
    returning(TrustManagerFactory.getInstance("X509")) { _.init(trustStore) }.getTrustManagers.head.asInstanceOf[X509TrustManager]
}
