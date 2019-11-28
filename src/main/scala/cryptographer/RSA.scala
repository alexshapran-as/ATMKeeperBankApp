package cryptographer

import java.security._
import javax.crypto.Cipher

object RSA {
  val cipher: Cipher = Cipher.getInstance("RSA")

  def getSelfPublicKey: PublicKey = if (selfPublicKey != null) selfPublicKey else { updateKeys; selfPublicKey }

  def decrypt(input: Array[Byte]): String = {
    cipher.init(Cipher.DECRYPT_MODE, privateKey)
    new String(cipher.doFinal(input))
  }

  def encrypt(input: String, publicKey: PublicKey): Array[Byte] = {
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    cipher.doFinal(input.getBytes)
  }

  private var (selfPublicKey, privateKey) = getKeys

  def updateKeys: Unit = {
    val keys = getKeys
    this.selfPublicKey = keys._1
    this.privateKey = keys._2
  }

  def resetKeys: Unit = {
    this.selfPublicKey = null
    this.privateKey = null
  }

  // Generate self RSA keys
  private def getKeys: (PublicKey, PrivateKey) = {
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(1024)
    val pair: KeyPair = generator.generateKeyPair()
    val publicKey: PublicKey = pair.getPublic()
    val privateKey: PrivateKey = pair.getPrivate()
    (publicKey, privateKey)
  }
}
