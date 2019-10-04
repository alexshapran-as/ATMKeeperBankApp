package configurations

import com.typesafe.config.{Config, ConfigFactory}

object Conf {
  val conf: Config = ConfigFactory.load("ATMKeeperBankApp_Configurations")
  val confSecretKey: String = conf.getString("conf.bankservice.secretKey")
  val confApiServiceInterface: String = conf.getString("conf.apiservice.interface")
  val confApiServicePort: Int = conf.getInt("conf.apiservice.port")
}
