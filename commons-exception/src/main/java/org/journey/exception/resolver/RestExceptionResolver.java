package org.journey.exception.resolver;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.journey.exception.core.BusinessException;
import org.journey.exception.core.RestExceptionConstants;
import org.journey.exception.model.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;


/**
 * @author wudan-mac
 * @ClassName: ExceptionResolver
 * @ClassNameExplain: 异常解析
 * @Description: 统一处理异常，json 适用于 rest api 工程使用
 * 这里没有使用注解注入
 * 考虑到这里有两个resolver 分别为 rest api工程 和 web工程服务
 * 在具体的工程中需要哪个就去引用哪个即可
 * @date 2016年4月1日 下午3:42:49
 */
public class RestExceptionResolver extends SimpleMappingExceptionResolver {

    private String contentType = "application/json;charset=UTF-8";

    private String encode = "UTF-8";

    public RestExceptionResolver() {
        //值越小，越先执行
        setOrder(-1000);
    }


    /**
     * @param
     * @return org.springframework.web.servlet.ModelAndView
     * @Title: doResolveException
     * @TitleExplain:
     * @Description: 包装异常信息的方法
     * @version
     * @author wudan-mac
     */
    @Override
    protected ModelAndView doResolveException(HttpServletRequest request,
                                              HttpServletResponse response, Object handler, Exception ex) {
        int code = 0;
        String errorMessage = "";

        if (ex instanceof BusinessException) {

            /**
             * 1.业务异常
             */
            BusinessException businessEx = (BusinessException) ex;
            code = businessEx.getCode();
            errorMessage = businessEx.getErrorMsg();
        } else if (ex instanceof MethodArgumentNotValidException) {

            /**
             * 2.springMVC 参数绑定异常
             */
            MethodArgumentNotValidException argumentEx = (MethodArgumentNotValidException) ex;
            code = RestExceptionConstants.ARGUMENT_ERROR_CODE;
            StringBuffer sb = new StringBuffer();
            List<FieldError> errorList = argumentEx.getBindingResult().getFieldErrors();
            for (FieldError error : errorList) {
                sb.append(error.getField());
                sb.append("字段");
                sb.append(error.getDefaultMessage());
            }
            errorMessage = sb.toString();
        } else if (ex instanceof HttpMessageNotReadableException) {

            /**
             * 3.参数反序列化异常
             */
            HttpMessageNotReadableException notReadEx = (HttpMessageNotReadableException) ex;
            code = RestExceptionConstants.ARGUMENT_ERROR_CODE;
            errorMessage = notReadEx.getMessage();
        } else if (ex instanceof UndeclaredThrowableException) {

            /**
             * 4.处理  undeclaredthrowableexception异常,目前只是GsonHttpMessageConverter转换时发生的异常
             */
            UndeclaredThrowableException undeclaredThrowableException = (UndeclaredThrowableException)ex;
            code = RestExceptionConstants.ARGUMENT_ERROR_CODE;
            errorMessage = undeclaredThrowableException.getMessage();
        } else if(ex instanceof JsonSyntaxException){
            /**
             * 5.处理  JsonSyntaxException,目前只是GsonHttpMessageConverter转换时发生的异常
             */
            JsonSyntaxException jsonSyntaxException = (JsonSyntaxException) ex;
            code = RestExceptionConstants.ARGUMENT_ERROR_CODE;

            errorMessage = jsonSyntaxException.getCause().getMessage();
        } else{
            code = RestExceptionConstants.SERVER_EXCEPTION_CODE;
            errorMessage = RestExceptionConstants.SERVER_EXCEPTION_MSG;
        }

        logger.error(ex);
        handleException(response, code, errorMessage);
        return null;
    }


    /**
     * @param response     响应
     * @param code         错误码
     * @param errorMessage 错误描述
     * @return boolean
     * @Title: handleException
     * @TitleExplain: 处理异常
     * @Description: 真正处理异常的地方, 将异常转为json返回给客户端
     * @author wudan-mac
     */
    private boolean handleException(HttpServletResponse response, int code, String errorMessage) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setCode(code);
            errorResponse.setMessage(errorMessage);
            output.write(new Gson().toJson(errorResponse).getBytes(encode));

            response.setStatus(code);
            response.setContentType(contentType);
            response.setContentLength(output.size());

            ServletOutputStream responseOutputStream = response.getOutputStream();
            output.writeTo(responseOutputStream);
            responseOutputStream.flush();
            response.flushBuffer();
            return true;
        } catch (IOException e) {
            String msg = "resolve business exception failed[biz_code=%d,biz_message=%s].";
            logger.error(String.format(msg, code, errorMessage), e);
        }
        return false;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEncode() {
        return encode;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }
}
