<#assign className = packager.packageName>
<#assign classNameLower = className?uncap_first>
package ${basepackage}.${packager.uncapPackageName}.service.impl;

<#include "/serviceImpl_imports.include">

@Service
public class ${className}ServiceImpl implements I${className}Service {

    static final Logger logger = LoggerFactory.getLogger(${className}ServiceImpl.class);

    @Override
    public ModelAndView ${classNameLower}(${className}IO ${classNameLower}IO, RedirectAttributes attr) throws Exception {

        ModelAndView modelAndView = new ModelAndView("${classNameLower}View");


        return modelAndView;
    }
}