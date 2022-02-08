# Guardian

> an easy but power security framework

# 使用

## 导包

```java
<dependency>
    <groupId>com.landao</groupId>
    <artifactId>guardian-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

​	目前没有上传到中央仓库，因为几乎每天更新好几个功能，可以git下来后自己安装，push上来的肯定都是可用的版本

## yaml

```yaml
guardian:
  token:
    private-key: 'toekn密钥'
```

## Service

```java

/**
 * UserTokenDTO 可以自定义在token中保存的信息
 */
@GuardianService//注意这里不是普通的Service
public class UserAuthorService extends TokenService<UserTokenDTO,Integer> {

    /**
     * 可以注入其他组件，增强功能
     */
    @Resource
    StudentService studentService;

    @Resource
    StudentRoleService studentRoleService;

    @Override
    public Set<String> getRoles() {
        //建议通过辅助service获取角色枚举后,利用Guardian工具类转换成字符串
    }

    public String getName(){
        return getTokenBean().getName();
    }

    //保证你的userId可用，如果没有登陆，会直接弹出异常，那就是你权限注解书写的问题
    public Integer getBuildId(){
        Student student = studentService.lambdaQuery().eq(Student::getStudentId, getUserId()).one();
        return student.getBuildId();
    }

}
```

这个时候就可以使用了！！！

## 简单使用

```java
    @RequiredRole(roles = {RoleConst.student,RoleConst.teacher})
    @ApiOperation("查看我的预约")
    @GetMapping("/history")
    public CommonResult<PageDTO<HistoryAppointment>> getHistory(@RequestParam(defaultValue = "1") Integer page,
                                                                      @RequestParam(defaultValue = "7") Integer limit) {
        CommonResult<PageDTO<HistoryAppointment>> result = new CommonResult<>();

        if (page <= 0 || limit <= 0) {
            return result.err("分页参数异常");
        }

        Integer studentId = userAuthorService.getUserId();

        PageDTO<HistoryAppointment> pageDTO = seatService.pageHistoryAppointments(page, limit, studentId);

        return result.body(pageDTO);
    }
```

## 支持线程上下文

​	如果你不喜欢这种注入service的方式，你可以使用`GuardianContext.getUserId()`来直接获取所有的上下文信心，我保证所有操作不会超过一行代码！而且线程上下文提供给你了几乎完备的工具类，你几乎可以用它来做关于登陆认证的所有事情

​	当然，GuardianService注入模式本质是就是一个自带登陆信息的你的普通userService的增强版。

## 权限表达式

> 前言

​	我们首先应该分清一个概念，就是什么样的字符串适合放在注解里面，什么样的字符串适合放到数据库里面。	我们这样来思考，放在注解里面的字符串是干什么的？两个角度看，有含有特殊权限的用户会访问我们的接口，另一个角度，我们的我们的接口操作了哪些资源。如果这样来看，我们的注解权限表达式应该是user:update，一般都是这种简单的，我们这里提供最简单的支持，分成与或两种形式，如果你校验user:add是否可以通过user:(!update|(!add&delete))|order:add更好，实际上需求没有那么复杂，都是简单简单的验证，再复杂一点的，都有涉及到数据库了业务层面了，所以我只做简单的，只针对实际业务情况来

> 数据库定义

​	为了你我的简单考虑，我们意见数据库这样设置权限，不管你是用角色关联权限，还是用用户来关联权限，当然我更推荐后者。user:add，user:*，!user:delete。这样，简单又明了。

> 注解定义

​	user:add，其他的一律不支持，甚至包括*通赔符号，因为这个没有实际的业务意义

> 自定义

​	如果你觉得我的权限处理不符合你的要求，你可以自定义，而且能够直接集成到框架里面

# 特色

## 简约而不简单

> 配置简约

1. 导入starter

2. 配置必要的token密钥

3. 继承功能丰富的tokenService

   其他统统不需要配置！！！

> 配置灵活

​	支持在tokenService**代码级别**满足你对认证的一切幻想

> 自定义切面顺序

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    GuardianProperties.Interceptor interceptor = guardianProperties.getInterceptor();
    registry.addInterceptor(guardianInterceptor).addPathPatterns("/**").order(interceptor.getOrder());
}
```

​	**注意**:直接在yaml配置就可以，这里是想告诉你怎么使用这个order(每个配置项我都生成了自动提示的功能)

## 自定义tokenBean

> 什么是tokenBean

​	我们会在token中保存用户的一些信息，其中必要的是用户的id，此外我们还可以保存比如用户姓名，用户头像等经常需要获取的信息。基于面向对象的思想，我们习惯把这些信息封装为一个对象。

> 特色功能

​	Guardian利用泛型反射技术来实现tokenBean和token的ORM映射。而且改良支持使用线程ThreadLocal来完成tokenBean和userId的随时获取，并且提供精准的泛型支持

> 注意

​	tokenBean不建议存放影响系统功能的字段，比如用户的收货地址，假如用户登陆后很多业务都会根据这个字段来判断，但是你不能把它存到token里面，因为一旦收货地址变动，用户就可以利用这个破坏我们的业务逻辑。

> 自定义额外字段

​	但是，我们的权限框架给出了一种新的解法，我们可以自己额外封装一个对象，重写setExtra方法，利用GuardianContext上下文设置额外字段，并且这个操作会在鉴权结束后自动执行，所以我们可以在其他地方获取这些额外信息。而且具有缓存的功能，效率非常高。

​	请结合业务逻辑谨慎考虑使用此功能，因为有的时候，你的这些额外字段，可能一次都不会被获取，白白浪费的线程资源。当然，你也可以利用GuardianContext上下文只有操控这个额外字段，它没有登陆检查限制，相当于一个会伴随你整个请求线程的线程专属变量，如果你愿意，这也是一个不错的工具类。

## 天然支持多用户鉴权

​	打破市面上多用户鉴权配置负责，鉴权恐难的框架。Guardian独辟蹊径，使用了完全不同的架构思路，继承TokenSevice，让我来编写通用的业务代码，让用户来实现自己的特殊要求，无论多少种用户，只需要无脑继承就可！

## 黑名单

​	token和session相比，唯一不足的地方就是不能直接的限制token失效。一般场景有，用户修改密码。不能说修改了密码之后，以前的token还是可以登陆。所以，我们这里提出了一种同时适合**分布式**，**多用户**且自由度高的黑名单机制。

​	我们以签发时间作为切入点，需要用户重写tokenService的一段逻辑，当然，我们后续打算接管这种逻辑。

​	我们在redis中以`default:2020217944`为key，default是用户的类型，2020217944是用户的id，value是一个时间戳，如果用户提供的token的时候戳小于这个的话，是不容许认证！这样的好处有什么，首先，我们不需要保存大量的token，因为一个时间就能卡死了。为什么可以有这样的设计，一切的设计都有过一段逻辑的思考。因为我们不可能容许一个比旧token还旧的token登陆。

​	而你只需要调用函数，传入过期的时间，就是这么简单，比如用户**退出登陆**，你把这个时间传入，再比如用户**修改密码**，你直接把这个token传入，我们会尽可能的重写足够的方法，让你实现一个函数加入黑名单。而且，可以直接实现**踢人下线**的功能，你只需要传入id，那么这个人的token就被禁封了！



## 智能提示

> yaml

​	所以的yaml配置，我们都会进行预先检测，常见的比如空字符串的检测，而且还会自动帮你削去字符串前后的空格，如果你配置错误，会收到非常友好的中文提示。

> 运行时检测

​	如果我们发现你在使用注解或其他功能的时候，会出现"程序员炒饭"现象，未能正确使用，我们也会通过简单的判断来查询出来，并告诉你正确的使用方法。

# 登陆和认证

​	下面聊一聊登陆和鉴权的一些概念和思考，以及他们在多用户环境下的一些思考。我会边聊这些问题的思考，边给出设计的理念和使用的方法，让你像看故事一样，学会注解的使用。

> 登陆

​	登陆是认证的前提，基于这个理念，Guardian的所有认证注解，包括角色认证和权限认证都是基于登陆前提的，换句话说：只要你在上面标注了认证相关的注解，不管是不是和这个用户类型有关的，我们都会要求用户登陆。

​	其实这个逻辑很简单，假如我有一个接口是为A类用户准备的，但我需要A有x权限。如果不限制必须登陆，那么我完全可以退出登陆，然后访问这个接口，这样的权限认证肯定是没有丝毫逻辑的。

​	在多用户类型的环境中，我们还需要考虑用户类型。我们假设有的接口可以让所有人都访问，比如获取应用的名称，这类接口和用户类型无关。然后，我们还有一个接口，是查看个人信息，只要是用户都可以查看个人信息。这个时候我们就要求所有人都登陆，使用注解吗？不，除非你保证不同类型用户返回的数据是一样的。那你说，我可以利用Guardian上下文判断呀。其实，这种的情况，分开写两个方法更好。

​	这就是多用户环境下的悖论吗？我们可以分开写接口。是的，但也不完全是这样。因此，我们的@RequiredLogin注解在你不加任何限制的情况下，默认就是，不管你是什么类型的用户，你都需要登陆。

​	那什么叫不完全是呢？好，我给你举一个例子，我们有一个文件上传的接口，这个接口只容许部分类型的用户上传文件。那你可能会想，这难道不是认证部分吗？我的回答是，可以是，也可以不是。嗯？什么意思？假如你这两种类型的用户，有一种用户完全不能上传文件，另一个用户都可以上传文件。那你就用@RequiredLogin，否则，你某种类型的用户，只有具有一部分权限的用户才能上传文件，那就使用认证的注释。而且，认证前比登陆的逻辑，你完全可以兼容前面的那种情况！

## 登陆验证

​	@RequiredLogin既可以标注在类上，又可以标注在方法上。依据就近原则，方法标注优先。

​	你可以这样记，在类上标注了，相当于给每个方法都标注了，如果同时标注了，方法覆盖类上的注解。

- 单种用户
   - 需要登陆
      - @RequireLogin
   - 不需要登陆
      - 不标注
- 多种用户(A,B,C....Z)
   - 需要登陆
      - 全部需要登陆
         - @RequireLogin
      - 只容许A,B使用
         - @RequireLogin(onlyFor={"A","B"})
      - 不容许B,C使用
         - @RequireLogin(forbidden={"B","C"})
   - 不需要登陆
      - 不标注

> 认证

​	这下看了，登陆是完全没有问题了，那认证呢？在多用户的环境的下，你继续要考虑角色，又需要考虑权限，还得兼顾不同用类型的不同角色，最最关键的是，有的时候角色和权限是混合在一起校验的，那这么复杂的逻辑，你能用注解表示清楚吗？那判断逻辑不得套个7，8层吗？

​	别急，我给你慢慢分析。你仔细想想，我们对接口的访问是不是可以看成对资源的一种操作？

​	你说的没有错，这不就是restful风格的接口吗？对，我们先来设想一个业务场景：学生保修宿舍的问题，维修部门指定谁去报销，修好了之后，让学生确认好评。现在你看，学生和维修部门对于我们来说，都是用户，而且他们要共同操作一个“资源”：维修。而且最关键的是，这两种用户之间的数据表结构肯定是不一样的，所以，我们必须要使用多用户了。而且为了业务更能提现框架的强大和智能，我们加一种用户类型，维修工人，维修工人负责接任务，修东西。

​	现在，我们有多种用户类型，多种角色和多种权限了。我们来思考几个问题。

​	首先是权限，我们来想，如果光有权限的话，是不是也能完成所有的认证逻辑。`报修:上报`，`报修:指定人员`，`报修:维修`，`报修:好评`，`报修:删除`......，这么想想好像确实也够了。而且，这些权限还是统一的，就是说，学生的`维修:删除`和部门`报修:删除`是一个权限，可能你会说，学生只能删除自己的，但是部门可以删除。。。打住打住，这个明显是业务逻辑的部分了，一个权限框架，怎么去知道怎么判断是否为用户自己的资源。

​	哦哦，原来权限是统一的呀，就像restful一样。那只弄权限不就行了嘛。当然可以了，但是这样系统的需要储存的权限也太多了，每个学生明明权限都一样，却要在数据里面储存一大堆。对呀，那怎么办，要是有一个东西能把这些权限结合起来就好了。等一下，那不就是角色吗？我们给学生指定学生的角色，学生就自动有这些所有的权限。

​	所以，我们在这里确定了两个基本逻辑，算上登陆那个，已经有三个了。权限对所有用户类型来说都是一样的。角色是权限的集合。等一下，等一下，就比如`user:delete`删除这个权限你怎么写呢？假如我有系统用户和学生用户。你自己其实已经说出来了，system-user:delete，student-user:delete，甚至你写三级分类都是可以的，逻辑上是一样的。再等一下，假如我想要有*-user:delete的权限呢，就比如我是超级管理员。小问题，我给你加上正则匹配。那假如我想写user:add和user:update，要两个一起写吗，不，你可以写成user:add,update，而且你这是用户的权限吧，谁会这么写接口呀。哦哦，有道理嘿，那。。。别那了，我直接把这个扩展的逻辑给你，先执行你的逻辑，你要是通过了，我就不验证了，你要是没通过，可以指定是否执行我的判断逻辑。那好啊，这太棒了！

​	所以我们注解认证的基本逻辑就来了，先验证权限，如果权限通过了，那就不验证了角色了。啊？为什么？那你想想，为什么我要指定权限？为什么？不是有角色就够了吗，自己知道这些角色能什么，指定就可以了。所以说，权限肯定是角色的一种特殊，比如我有一个角色是学生，但是我是这个系统的开发者唉，让我看看用户登陆的情况不过分吧，那你怎么办？难道给我管理员的角色吗？万一我那天把全校的学生都删了呢？那。。给你一个角色专门查看登陆情况吧，等一下，这不就是权限吗？哦哦，原来权限是在角色的基础上富裕用户的一些特殊行为呀，这下我理解了。等一下，还没结束呢，我们可以是唯一一个天然支持多用户类型的框架，那多用户类型呢？你可别忘了，权限这个核心概念就告诉你，权限和用户类型是无关的！但是角色和用户类型是有关的，学生的管理员和教师的管理员，那权限差了远了。

​	我懂了，是不是我们的注解是这样设计的，一个权限注解指定了权限，然后里面套多角色注解，如果权限通过，就不管角色了，如果权限没有通过，那我就一个一个验证用户。对不对！我只能说，逻辑上对，但是你这样写起来太复杂了，我直接把这个两个注解分开了，一个是单个的@RequiredPermission然后是@RequiredRole和@RequiredRoles你标注一个，如果你针对默认用户，那就标注前一个，否则，你就标注后一个，后一个就是把前一个嵌套在一起了。而且这样做还有一个好处，就是方便扩展。比如你定义了一个接口需要管理员角色，但是你怎么也没想到，有个人想要查看登陆用户这个权限，难道我把这个注解套到里面吗？不。我直接加一个@RequiredPermission就行。然后，我发现这小子利用这个权限干坏事，我不能把这个权限给任何人了，我直接把那删掉。

​	哦，那这样太完美了。等一下，我还想到一个问题，权限禁封，假如我这个角色的某个用户被禁封某种权限呢，那也可以，我们直接加到权限里。等一下，怎么加？很简单，加个符号!user:add，我们把这个加到数据库里面，然后拿出来匹配，甚至我们的注解都不用改，我们加个判断逻辑就可以了。

​	秒啊，那，你@RequiredPermission打算弄成怎么样呢，弄一个还是弄多个。这个还真需要仔细考虑一下呢。首先，我们用户的权限可能有多个，有的是通过，有的是禁止(等一下，我打算做一个封号的功能，调用service接口来判断是否被封号！而且我要支持自定义@Handler，放在切面的位置去处理)好了回到正题。我们好好想想，我们有一个接口，需要多个权限表达式，至少，我们需要操控的得不止一个数据库吧，比如删除订单。。。

​	怎么样，激动人心吗？

## 权限验证

- 已登陆->获取userType
  - 查看是否标注了@RequiredPermission注解
    - 标注了
      - 查看自己的权限是否能通过这些验证
        - 被禁止了->禁止访问
        - 通过了->访问接口
        - 没有通过->继续往下看角色注解
    - 没有标注
      - 看角色注解
  - 标注了任意与角色相关的注解
    - 按照:方法优先于类，复合优先于单注解的顺序，寻找和userType匹配的注解
      - 找到了
        - 依据逻辑判断类型开始匹配
          - or:符合其中一个就可以访问
          - and:全部符合才可以访问
          - not:禁止**只含有**指定角色的访问(如果我们的角色比not指定的多，我就可以通过
          - 接口，自定义实现逻辑
      - 未找到
        - 禁止访问接口
  - 未标注任何与角色相关的注解
    - 可以访问接口
- 未登录
  - 标注任意与权限相关的注解
    - 禁止访问接口
  - 未标注任何于权限相关的注解
    - 可以访问接口

# TokenBean ORM

> 主键类型

​	常用的Integer,Long,String统统支持。一个@UserId系统就会帮你自动识别而判断

> 其他类型

- [ ] 枚举类型
- [ ] 日期类型(支持java8新api
- [ ] 集合类型
- [ ] 嵌套类型

> 灵活ORM

- [ ] 用注解避开反序列化
- [ ] 用transient关键字避开反序列化

# 高性能

> 权限认证懒加载

​	采用二重并发锁保证安全性的前提下，能不查询数据库权限就不查询数据库权限。当然，你利用Guardian上下文自定义代码级别的权限校验也是可以相同的原理。非必要不查询数据库，是我们高性能的保障之一

> 线程缓存技术

​	能使用ThreadLocal缓存的数据信息，我都会使用ThreadLocal来缓冲，甚至包括你的当前认证的service

> 自定义id

​	利用bean id直接从spring容器中获取，省去一次循环遍历，虽然可能只需要循环遍历不到2个元素。但，这就是我们为高性能所做的努力

# 自定义配置

## 跨域

​	默认自动配置，解决cors问题，但是如果你配置了，我的配置就不会生效，另外提供yaml自定义

## 自定义Handler

​	在完成注解校验后，框架会执行所有用户自己定义的Handler，而用户只需要继承`GuardianHandler`接口

```java
public interface GuardianHandler extends Ordered {

    /**
     * 返回值决定是否继续往下拦截
     * @param method 你要执行的controller中的方法
     */
    boolean handler(Method method);
    
    @Override
    default int getOrder(){
        return 0;
    }

}
```

​	记得把自己定义的Handler加入spring的bean中，只要一个@Compent注解就可以了

## TODO

- [ ] 策略模式,自定义tokenBean转换规则
- [ ] 钩子函数AOP切入，为所以可能的地方提供AOP
- [ ] 日志功能,完善的鉴权日志流程
- [ ] 自定义鉴权逻辑