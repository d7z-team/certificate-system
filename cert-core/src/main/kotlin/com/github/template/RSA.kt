package com.github.template

import java.math.BigInteger
import java.util.Base64
import java.util.Random

class RSA {
    // 获取最大公约数
    private fun getGCD(a: BigInteger, b: BigInteger): BigInteger {
        return if (b.toByte().toInt() == 0) a else getGCD(b, a.mod(b))
    }

    // 密匙对
    class SecretKey(var n: BigInteger, var e: BigInteger, var d: BigInteger) {
        val publicKey: PublicKey
            get() = PublicKey(n, e)
        val privateKey: PrivateKey
            get() = PrivateKey(n, d)

        // 密钥
        class PrivateKey(var n: BigInteger, var d: BigInteger)

        // 公钥
        class PublicKey(var n: BigInteger, var e: BigInteger)
    }

    companion object {
        private const val numLength = 1024 // 素数长度
        private const val accuracy = 100 // 素数的准确率为1-(2^(-accuracy))

        // 扩展欧几里得方法,计算 ax + by = 1中的x与y的整数解（a与b互质）
        private fun extGCD(a: BigInteger, b: BigInteger): Array<BigInteger> {
            return if (b.signum() == 0) {
                arrayOf(a, BigInteger("1"), BigInteger("0"))
            } else {
                val bigIntegers = extGCD(b, a.mod(b))
                val y = bigIntegers[1].subtract(a.divide(b).multiply(bigIntegers[2]))
                arrayOf(bigIntegers[0], bigIntegers[2], y)
            }
        }

        // 超大整数超大次幂然后对超大的整数取模，利用蒙哥马利乘模算法,
        // (base ^ exp) mod n
        // 依据(a * b) mod n=(a % n)*(b % n) mod n
        private fun expMode(base: BigInteger, exp: BigInteger, mod: BigInteger): BigInteger {
            var res = BigInteger.ONE
            // 拷贝一份防止修改原引用
            var tempBase = BigInteger(base.toString())
            // 从左到右实现简答
            /*
            D=1
            WHILE E>=0
        　　  IF E%2=0
        　　      C=C*C % N
        　　  E=E/2
        　　ELSE
        　　  D=D*C % N
        　　  E=E-1
        　　RETURN D
        */for (i in 0 until exp.bitLength()) {
                if (exp.testBit(i)) { // 判断对应二进制位是否为1
                    res = res.multiply(tempBase).mod(mod)
                }
                tempBase = tempBase.multiply(tempBase).mod(mod)
            }
            return res
        }

        // 产生公钥与私钥
        fun generateKey(p: BigInteger, q: BigInteger): SecretKey {
            // 令n = p * q。取 φ(n) = (p-1) * (q-1)。
            val n = p.multiply(q)
            // 计算与n互质的整数个数 欧拉函数
            val fy = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
            // 取 e ∈ [1 < e < φ(n) ] ，( n , e )作为公钥对，这里取65537
            val e = BigInteger("65537")
            // 计算ed与fy的模反元素d。令 ed mod φ(n)  = 1，计算d，然后将( n , d ) 作为私钥对
            val bigIntegers = extGCD(e, fy)
            // 计算出的x不能是负数，如果是负数，则进行x=x+fy。使x为正数，但是x<fy。
            var x = bigIntegers[1]
            if (x.signum() == -1) {
                x = x.add(fy)
            }
            // 返回计算出的密钥
            return SecretKey(n, e, x)
        }

        fun generateKey(): SecretKey {
            val pq = randomPQ
            return generateKey(pq[0], pq[1])
        }

        // 加密
        fun encrypt(text: BigInteger, publicKey: SecretKey.PublicKey): BigInteger {
            return expMode(text, publicKey.e, publicKey.n)
        }

        // 解密
        fun decrypt(cipher: BigInteger, privateKey: SecretKey.PrivateKey): BigInteger {
            return expMode(cipher, privateKey.d, privateKey.n)
        }

        // 加密
        fun encrypt(text: String, publicKey: SecretKey.PublicKey): String {
            return encrypt(BigInteger(text.toByteArray()), publicKey).toString()
        }

        // 解密
        fun decrypt(chipper: String?, privateKey: SecretKey.PrivateKey): String {
            val bigInteger = expMode(BigInteger(chipper), privateKey.d, privateKey.n)
            val bytes = ByteArray(bigInteger.bitLength() / 8 + 1)
            for (i in bytes.indices) {
                for (j in 0..7) {
                    if (bigInteger.testBit(j + i * 8)) {
                        bytes[bytes.size - 1 - i] = (bytes[bytes.size - 1 - i].toInt() or (1 shl j)).toByte()
                    }
                }
            }
            return String(bytes)
        }

        // 产生两个随机1024位大质数
        val randomPQ: Array<BigInteger>
            get() {
                var p = BigInteger.probablePrime(numLength, Random())
                while (!p.isProbablePrime(accuracy)) {
                    p = BigInteger.probablePrime(numLength, Random())
                }
                var q = BigInteger.probablePrime(numLength, Random())
                while (!q.isProbablePrime(accuracy)) {
                    q = BigInteger.probablePrime(numLength, Random())
                }
                return arrayOf(p, q)
            }

        @JvmStatic
        fun main(args: Array<String>) {
            val secretKey = generateKey()
            // 明文内容不要超过1024位,超过后需要分段加密
            val text = "Hello world"
            val chipper = encrypt(text, secretKey.publicKey)
            println(
                """
    加密后:
    密文二进制长度:${BigInteger(chipper).bitLength()}
    ${Base64.getEncoder().encodeToString(chipper.encodeToByteArray())}
                """.trimIndent()
            )
            val origin = decrypt(chipper, secretKey.privateKey)
            println("解密后:\n$origin")
        }
    }
}
