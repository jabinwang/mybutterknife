package com.jabin.mybk.compiler;

import com.google.auto.service.AutoService;
import com.jabin.mybk.annotation.BindView;
import com.jabin.mybk.annotation.OnClick;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@AutoService(Processor.class)

public class ButterKnifeProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    private Map<TypeElement, List<VariableElement>> bindViewMap = new HashMap<>();
    private Map<TypeElement, List<ExecutableElement>> onClickMap = new HashMap<>();
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(Constants.BINDVIEW_ANNOTATION_TYPE);
        set.add(Constants.ONCLICK_ANNOTATION_TYPE);
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (!EmptyUtils.isEmpty(set)){
            //获取所有BindView的注解集合
            Set<? extends Element> bindViewElement = roundEnvironment.getElementsAnnotatedWith(BindView.class);
            //获取所有OnClick的注解集合
            Set<? extends Element> onClickElement = roundEnvironment.getElementsAnnotatedWith(OnClick.class);

            parseBindView(bindViewElement);
            parseOnClick(onClickElement);

            createJavaFile();

        }

        return false;
    }

    private void parseBindView(Set<? extends Element> set) {
        if (!EmptyUtils.isEmpty(set)){
            //跳过一些校验
            for (Element e :
                    set) {
                if (e.getKind() == ElementKind.FIELD) {
                    VariableElement var = (VariableElement) e;
                    TypeElement typeElement = (TypeElement) e.getEnclosingElement();
                    if (bindViewMap.containsKey(typeElement)){
                        bindViewMap.get(typeElement).add(var);
                    }else {
                        List<VariableElement> list = new ArrayList<>();
                        list.add(var);
                        bindViewMap.put(typeElement, list);
                    }
                }
            }
        }
    }

    private void parseOnClick(Set<? extends Element> set) {
        if (!EmptyUtils.isEmpty(set)){
            //跳过一些校验
            for (Element e :
                    set) {
                if (e.getKind() == ElementKind.METHOD) {
                    ExecutableElement var = (ExecutableElement) e;
                    TypeElement typeElement = (TypeElement) e.getEnclosingElement();
                    if (onClickMap.containsKey(typeElement)){
                        onClickMap.get(typeElement).add(var);
                    }else {
                        List<ExecutableElement> list = new ArrayList<>();
                        list.add(var);
                        onClickMap.put(typeElement, list);
                    }
                }
            }
        }
    }

    private void createJavaFile(){
        if (!EmptyUtils.isEmpty(bindViewMap)){
            //获取ViewBinder接口
            TypeElement viewBinderType = elementUtils.getTypeElement(Constants.VIEW_BINDER);
            //获取clicklistener
            TypeElement clickListenerType = elementUtils.getTypeElement(Constants.ONCLICKLISTENER);

            //获取view type
            TypeElement viewType = elementUtils.getTypeElement(Constants.VIEW);

            for (Map.Entry<TypeElement, List<VariableElement>> entry:
                 bindViewMap.entrySet()) {
                ClassName className = ClassName.get(entry.getKey());
                //实现接口泛型
                ParameterizedTypeName parameterizedTypeName =
                        ParameterizedTypeName.get(
                                ClassName.get(viewBinderType),
                                ClassName.get(entry.getKey())
                                );
                ParameterSpec parameterSpec = ParameterSpec.builder(
                        ClassName.get(entry.getKey()),
                        Constants.TARGET_PARAMETER_NAME,
                        Modifier.FINAL).build();
                MethodSpec.Builder methodBuiler = MethodSpec.
                        methodBuilder(Constants.METHOD_BIND_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(parameterSpec);

                for (Element e:
                     entry.getValue()) {
                    //获取属性名
                    String fieldName = e.getSimpleName().toString();
                    int value = e.getAnnotation(BindView.class).value();
                    //target.tv = target.findViewById(R.id.tv)
                    String methodContent = "$N." + fieldName + "= $N.findViewById($L)";
                    methodBuiler.addStatement(methodContent,
                            Constants.TARGET_PARAMETER_NAME,
                            Constants.TARGET_PARAMETER_NAME,
                            value);
                }
                if(!EmptyUtils.isEmpty(onClickMap)){
                    for (Map.Entry<TypeElement, List<ExecutableElement>> methodEntry:
                         onClickMap.entrySet()) {
                        if (className.equals(ClassName.get(entry.getKey()))){
                            for (Element e:
                                 methodEntry.getValue()) {
                                String methodName = e.getSimpleName().toString();
                                int val = e.getAnnotation(OnClick.class).value();
                                /**
                                 * target.findViewById(111).setOnClickListener(new BeBouncingOnClickListener(){
                                 *     public void doClick(View view){
                                 *         target.click(view);
                                 *     };
                                 * };
                                 */

                                methodBuiler.
                                        beginControlFlow(
                                                "$N.findViewById($L).setOnClickListener(new $T()",
                                                Constants.TARGET_PARAMETER_NAME,
                                                val,
                                                ClassName.get(clickListenerType)
                                                )
                                        .beginControlFlow(
                                                "public void doClick($T view)",
                                                ClassName.get(viewType))
                                        .addStatement(
                                                "$N."+ methodName+"(view)",
                                                Constants.TARGET_PARAMETER_NAME)
                                        .endControlFlow()
                                        .endControlFlow(")")
                                        .build();
                            }
                        }
                    }
                }

                try {
                    JavaFile.builder(
                            className.packageName(),
                            TypeSpec.classBuilder(className.simpleName()+"$ViewBinder")
                            .addSuperinterface(parameterizedTypeName)
                            .addModifiers(Modifier.PUBLIC)
                            .addMethod(methodBuiler.build())
                            .build()
                            ).build()
                            .writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



        }
    }
}
