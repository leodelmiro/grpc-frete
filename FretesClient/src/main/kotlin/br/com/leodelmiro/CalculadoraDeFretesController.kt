package br.com.leodelmiro

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import javax.inject.Inject

@Controller
class CalculadoraDeFretesController(@Inject val gRpcClient: FretesServiceGrpc.FretesServiceBlockingStub) {

    @Get("/api/fretes")
    fun calcula(@QueryValue cep: String): FreteResponse {

        val request = CalculaFreteRequest.newBuilder()
            .setCep(cep)
            .build()


        try {
            var response = gRpcClient.calculaFrete(request)

            return FreteResponse(
                cep = response.cep,
                valor = response.valor
            )
        } catch (e: StatusRuntimeException) {
            val status = e.status
            val statusCode = status.code
            val description = status.description

            if (statusCode == Status.Code.INVALID_ARGUMENT) {
                throw HttpStatusException(HttpStatus.BAD_REQUEST, description)
            }

            if (statusCode == Status.Code.PERMISSION_DENIED) {

                val statusProto = StatusProto.fromThrowable(e)

                if (statusProto == null) {
                    throw HttpStatusException(HttpStatus.FORBIDDEN, description)
                }

                val anyDetails = statusProto.detailsList.get(0)
                val details = anyDetails.unpack(ErrorDetails::class.java)

                throw HttpStatusException(HttpStatus.FORBIDDEN, "${details.code}: ${details.message}")
            }

            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    data class FreteResponse(val cep: String, val valor: Double)
}