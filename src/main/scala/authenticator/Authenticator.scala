package authenticator

import configurations.Conf.confSecretKey
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Authenticator {

  def generateHMAC(preHashString: String): String = {

    val secret: SecretKeySpec = new javax.crypto.spec.SecretKeySpec(confSecretKey.getBytes("UTF-8"), "HmacSHA256")
    val mac: Mac = javax.crypto.Mac.getInstance("HmacSHA256")
    mac.init(secret)
    val result: Array[Byte] = mac.doFinal(preHashString.replaceAll("\n", "").replaceAll("\\s", "").getBytes("UTF-8"))
    new sun.misc.BASE64Encoder().encode(result)

  }

  def checkSignatures(commandSignature: String, command: String): Either[String, String] = {

    if (commandSignature == generateHMAC(command)) {
     Right("Электронная подпись команды верна")
    } else {
      Left("Отказано в доступе. Электронная подпись команды неверна")
    }

  }

}
